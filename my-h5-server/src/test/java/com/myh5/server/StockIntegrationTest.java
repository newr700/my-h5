package com.myh5.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【V2 第一步：防超卖】库存专项集成测试。
 *
 * ── 为什么这些用例值得写 ──────────────────────────────────
 * 「超卖」是典型的【单线程跑不出来】的 bug：手工点一百次都不一定碰到，
 * 但真实并发一来必现。这类 bug 有个特点 —— 靠人肉 review 很难发现，
 * 因为代码逻辑看着完全正确（"先看看剩几张，够就卖"）。
 *
 * 所以这里测的不是「功能对不对」，而是【不变量成不成立】：
 *   · 票不会卖成负数；
 *   · 扣不到的库存，一分都不会扣；
 *   · 没成交的订单，票要还得回来。
 * 这三条是「账能不能平」的底线，写测试锁住它们，以后谁改代码都不敢随便动。
 *
 * ── 关于测试隔离（这是踩过坑后的写法）────────────────────
 * 库存是【共享可变状态】：上一个用例扣掉的票会残留给下一个用例。
 * 第一版这里挂过一次（用例继承了前一条的库存副作用，断言莫名其妙失败）。
 * 现在每个用例开头都自己 resetStock() ——
 * 多花两行，换的是「用例可以任意单独跑、任意调整顺序」的自由。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper om = new ObjectMapper();

    private static final long MATCH_ID = 1;
    private static String token;

    // ── 辅助方法 ─────────────────────────────────────────────

    private int stockOf(long matchId) {
        Integer v = jdbcTemplate.queryForObject(
                "SELECT stock FROM match_info WHERE id = ?", Integer.class, matchId);
        return v == null ? 0 : v;
    }

    /** 每个用例自己摆好初始库存，杜绝用例之间的隐式耦合 */
    private void resetStock(long matchId, int stock) {
        jdbcTemplate.update("UPDATE match_info SET stock = ?, total_stock = ? WHERE id = ?",
                stock, stock, matchId);
    }

    private JsonNode call(String json) throws Exception {
        // V2 Step2：下单必须带幂等号 requestId。这里统一兜底——若调用方没传就自动补一个
        // 随机 UUID（每次唯一），避免漏传时整组库存用例被 @NotBlank 校验拦成 1001。
        String body = json.contains("requestId")
                ? json
                : json.replace("}", ",\"requestId\":\"" + java.util.UUID.randomUUID() + "\"}");
        MvcResult result = mockMvc.perform(post("/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString());
    }

    // ── 用例 ─────────────────────────────────────────────────

    @Test
    @Order(1)
    void 比赛列表带上库存字段_金额仍是分为单位() throws Exception {
        resetStock(MATCH_ID, 500);

        MvcResult result = mockMvc.perform(get("/matches"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());

        JsonNode m = body.get("data").get(0);
        assertEquals(500, m.get("totalStock").asInt(), "总票数应出现在契约里");
        assertEquals(500, m.get("stock").asInt(), "剩余票数应出现在契约里");
        // 顺带守住金额单位 —— 契约里的数据单位说变就变，是最容易出错的一处
        assertTrue(m.get("unitPrice").asInt() > 100, "单价应是以「分」为单位");
    }

    @Test
    @Order(2)
    void 注册用户拿到token() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"stock-tester\",\"password\":\"123456\",\"nickname\":\"库存测试员\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        token = body.get("data").get("token").asText();
    }

    @Test
    @Order(3)
    void 下单成功_库存按购买数量减少() throws Exception {
        resetStock(MATCH_ID, 500);

        JsonNode body = call("{\"matchId\":" + MATCH_ID + ",\"quantity\":3}");
        assertEquals(0, body.get("code").asInt());

        assertEquals(497, stockOf(MATCH_ID), "买 3 张后应剩 497 张");
    }

    @Test
    @Order(4)
    void 库存不足时_返回3003且一张都不扣() throws Exception {
        // 故意只留 1 张，却要买 5 张
        resetStock(MATCH_ID, 1);

        JsonNode body = call("{\"matchId\":" + MATCH_ID + ",\"quantity\":5}");

        assertEquals(3003, body.get("code").asInt(), "库存不足应返回专门的错误码 3003");
        assertEquals(1, stockOf(MATCH_ID),
                "扣减失败时库存必须原封不动 —— 这条断言守的是「不会凭空少票」");
    }

    @Test
    @Order(5)
    void 取消订单_库存回补给票池() throws Exception {
        resetStock(MATCH_ID, 100);

        JsonNode created = call("{\"matchId\":" + MATCH_ID + ",\"quantity\":4}");
        assertEquals(0, created.get("code").asInt());
        assertEquals(96, stockOf(MATCH_ID), "下单后应剩 96");

        long oid = created.get("data").get("id").asLong();
        mockMvc.perform(post("/order/" + oid + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertEquals(100, stockOf(MATCH_ID),
                "取消后票要还给票池，否则用户反复下单取消就能把票耗光");
    }

    @Test
    @Order(6)
    void 已支付的订单不能取消_库存不回补() throws Exception {
        resetStock(MATCH_ID, 200);

        JsonNode created = call("{\"matchId\":" + MATCH_ID + ",\"quantity\":2}");
        long oid = created.get("data").get("id").asLong();

        mockMvc.perform(post("/order/" + oid + "/pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult cancelResult = mockMvc.perform(post("/order/" + oid + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(3002, om.readTree(cancelResult.getResponse().getContentAsString()).get("code").asInt(),
                "已支付的订单不允许取消（状态机守卫）");

        assertEquals(198, stockOf(MATCH_ID),
                "票真卖出去了，取消失败就不该退票 —— 否则等于白送");
    }
}
