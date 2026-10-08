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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【V2 第二步：幂等 / 防重复下单】专项集成测试。
 *
 * ── 为什么这条值得锁成测试 ────────────────────────────────
 * 幂等是「看不见但会出资损」的那类正确性：单线程永远看不出问题，
 * 只有「同一笔请求到了两次」才暴露。这里直接用同一个 requestId 发两次，
 * 锁住两条底线：① 只建一笔订单；② 只扣一次库存。
 *
 * ── 隔离（沿用 StockIntegrationTest 的教训）──────────────
 * 库存是共享可变状态，每个用例开头自己 resetStock，互不污染。
 * 用户也各自注册（@NotBlank 的 requestId 与用户名都唯一），避免跨类/跨用例耦合。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper om = new ObjectMapper();

    private static final long MATCH_ID = 1;
    private static String token;

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

    /** 用指定 requestId 下一笔订单（qty 张），返回已解析的响应体 */
    private JsonNode create(String requestId, int qty) throws Exception {
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
        // 用户名必须落在 4-32 位（RegisterRequest 的 @Size 约束），否则注册会被校验拦下
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"idem-tester\",\"password\":\"123456\",\"nickname\":\"幂等测试员\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        token = body.get("data").get("token").asText();
    }

    @Test
    @Order(2)
    void 同一requestId重复下单_只建一笔且不重复扣库存() throws Exception {
        resetStock(MATCH_ID, 500);
        String rid = "same-request-1";

        JsonNode first = create(rid, 2);
        assertEquals(0, first.get("code").asInt());
        String firstOrderNo = first.get("data").get("orderNo").asText();

        JsonNode second = create(rid, 2);
        assertEquals(0, second.get("code").asInt());
        assertEquals(firstOrderNo, second.get("data").get("orderNo").asText(),
                "第二次应返回第一次的订单（幂等命中）");

        // 两次下单，但库存只扣一次（2 张）—— 不能扣成 496
        assertEquals(498, stockOf(MATCH_ID),
                "幂等命中时绝不能重复扣库存，否则用户没多买、库存却凭空少了");
    }

    @Test
    @Order(3)
    void 不同requestId_建两笔并各扣一次库存() throws Exception {
        resetStock(MATCH_ID, 500);
        create("req-a", 1);
        create("req-b", 1);
        assertEquals(498, stockOf(MATCH_ID),
                "不同的幂等号是两次独立的下单意图，应各自扣库存");
    }

    @Test
    @Order(4)
    void 缺少requestId_校验失败且绝不建单() throws Exception {
        resetStock(MATCH_ID, 500);
        // 故意不传 requestId，@NotBlank 应在进入业务前拦截。
        // 注意：本项目约定「业务失败 HTTP 仍是 200，成败由 code 表达」（见 GlobalExceptionHandler），
        // 所以校验失败返回 200 + 1001，不是 400。
        String json = "{\"matchId\":" + MATCH_ID + ",\"quantity\":1}";
        MvcResult result = mockMvc.perform(post("/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1001, om.readTree(result.getResponse().getContentAsString()).get("code").asInt(),
                "缺 requestId 应被 @NotBlank 拦截，返回 1001");
        assertEquals(500, stockOf(MATCH_ID),
                "校验失败不应建单、更不应扣库存");
    }

    @Test
    @Order(5)
    void 空requestId_同样校验失败() throws Exception {
        resetStock(MATCH_ID, 500);
        String json = "{\"matchId\":" + MATCH_ID + ",\"quantity\":1,\"requestId\":\"\"}";
        MvcResult result = mockMvc.perform(post("/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1001, om.readTree(result.getResponse().getContentAsString()).get("code").asInt(),
                "空串 requestId 同样被 @NotBlank 拦截");
        assertEquals(500, stockOf(MATCH_ID));
    }
}
