package com.myh5.server;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myh5.server.order.AuditLogEntity;
import com.myh5.server.order.AuditLogMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【V2 第三步：审计日志】专项集成测试。
 *
 * 验证三件事：
 * ① 成功的关键操作（下单 / 支付 / 取消）都会留下 result=1 的审计记录，且字段正确；
 * ② 失败的关键操作（状态机守卫 3002 / 订单不存在 3001）也会留下 result=0 的审计记录；
 * ③ 审计记录的字段（动作、状态迁移、错误码、幂等号）与操作一一对应。
 *
 * ── 隔离（沿用前两步的教训）──────────────────────────────
 * 库存各自 resetStock；审计断言全部按「唯一的 requestId / orderId / 幽灵 id」
 * 精确命中，不受其他测试类留下的审计行干扰（审计记录因 REQUIRES_NEW 会真实落库）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditLogMapper auditLogMapper;

    private final ObjectMapper om = new ObjectMapper();

    private static final long MATCH_ID = 1;
    private static String token;

    private void resetStock(long matchId, int stock) {
        jdbcTemplate.update("UPDATE match_info SET stock = ?, total_stock = ? WHERE id = ?",
                stock, stock, matchId);
    }

    /** 下一笔订单并返回已解析的响应体（带唯一 requestId） */
    private JsonNode createOrder(String requestId, int qty) throws Exception {
        String json = "{\"matchId\":" + MATCH_ID + ",\"quantity\":" + qty
                + ",\"requestId\":\"" + requestId + "\"}";
        MvcResult result = mockMvc.perform(post("/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @Order(1)
    void 注册用户拿到token() throws Exception {
        // 用户名必须落在 4-32 位（RegisterRequest 的 @Size 约束）
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"audit-tester\",\"password\":\"123456\",\"nickname\":\"审计测试员\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        token = body.get("data").get("token").asText();
    }

    @Test
    @Order(2)
    void 下单成功_留下审计成功记录() throws Exception {
        resetStock(MATCH_ID, 500);
        String rid = "audit-create-1";
        JsonNode body = createOrder(rid, 2);
        assertEquals(0, body.get("code").asInt());
        long orderId = body.get("data").get("id").asLong();

        AuditLogEntity a = auditLogMapper.selectOne(new QueryWrapper<AuditLogEntity>()
                .eq("request_id", rid).eq("action", "CREATE_ORDER"));
        assertNotNull(a, "下单成功必须留下审计记录");
        assertEquals(1, a.getResult(), "成功 → result=1");
        assertEquals("pending", a.getAfterStatus(), "新建订单后状态为 pending");
        assertNull(a.getBeforeStatus(), "新建没有前态");
        assertNull(a.getErrorCode(), "成功时错误码为 NULL");
        assertEquals(orderId, a.getTargetId().longValue(), "审计 target_id 应指向这笔订单");
    }

    @Test
    @Order(3)
    void 支付成功_留下状态迁移审计() throws Exception {
        resetStock(MATCH_ID, 500);
        long oid = createOrder("audit-pay-1", 1).get("data").get("id").asLong();

        mockMvc.perform(post("/order/" + oid + "/pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        AuditLogEntity a = auditLogMapper.selectOne(new QueryWrapper<AuditLogEntity>()
                .eq("action", "PAY").eq("target_id", oid));
        assertNotNull(a);
        assertEquals(1, a.getResult());
        assertEquals("pending", a.getBeforeStatus(), "支付前是 pending");
        assertEquals("paid", a.getAfterStatus(), "支付后是 paid");
        assertNull(a.getErrorCode());
    }

    @Test
    @Order(4)
    void 取消成功_留下状态迁移审计() throws Exception {
        resetStock(MATCH_ID, 500);
        long oid = createOrder("audit-cancel-1", 1).get("data").get("id").asLong();

        mockMvc.perform(post("/order/" + oid + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        AuditLogEntity a = auditLogMapper.selectOne(new QueryWrapper<AuditLogEntity>()
                .eq("action", "CANCEL").eq("target_id", oid));
        assertNotNull(a);
        assertEquals(1, a.getResult());
        assertEquals("pending", a.getBeforeStatus(), "取消前是 pending");
        assertEquals("closed", a.getAfterStatus(), "取消后是 closed");
    }

    @Test
    @Order(5)
    void 已支付订单再支付_失败也留痕_记3002() throws Exception {
        resetStock(MATCH_ID, 500);
        long oid = createOrder("audit-payfail-1", 1).get("data").get("id").asLong();
        // 第一次支付：成功
        mockMvc.perform(post("/order/" + oid + "/pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 第二次支付：应被状态机守卫拦截，返回 3002
        MvcResult r = mockMvc.perform(post("/order/" + oid + "/pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(3002, om.readTree(r.getResponse().getContentAsString()).get("code").asInt(),
                "已支付的订单不允许再支付（状态机守卫）");

        // 失败的支付也要留下审计：result=0, error_code=3002
        AuditLogEntity a = auditLogMapper.selectOne(new QueryWrapper<AuditLogEntity>()
                .eq("action", "PAY").eq("target_id", oid).eq("result", 0));
        assertNotNull(a, "失败的支付操作必须留下审计记录");
        assertEquals(3002, a.getErrorCode().intValue());
    }

    @Test
    @Order(6)
    void 支付不存在的订单_失败也留痕_记3001() throws Exception {
        long ghostId = 9876543210L; // 明确不存在的订单 id，且与其他测试不冲突
        MvcResult r = mockMvc.perform(post("/order/" + ghostId + "/pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(3001, om.readTree(r.getResponse().getContentAsString()).get("code").asInt(),
                "支付不存在的订单应返回 3001");

        AuditLogEntity a = auditLogMapper.selectOne(new QueryWrapper<AuditLogEntity>()
                .eq("action", "PAY").eq("target_id", ghostId).eq("result", 0));
        assertNotNull(a, "失败的支付操作必须留下审计记录");
        assertEquals(3001, a.getErrorCode().intValue());
    }
}
