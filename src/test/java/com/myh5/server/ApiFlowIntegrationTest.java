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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端集成测试 —— 把整个「写操作闭环」当作一条链路来测：
 *
 *   注册 → 登录 → 带 token 查资料 → 下单 → 查我的订单 → 支付 → 再取消（应被拒）
 *
 * 外加三条「坏路径」：不带 token、错误密码、篡改金额不可行。
 *
 * ── 为什么用 MockMvc 而不是真的起 HTTP 服务 ──────────────
 * MockMvc 在内存里模拟整个 Servlet 容器：拦截器、参数校验、异常处理器全部生效，
 * 但不需要端口、不需要网络 —— 快且稳定，跑的是和真实请求完全相同的代码路径。
 *
 * ── 测试环境 ────────────────────────────────────────────
 * src/test/resources/application.yml 把数据源切到 H2 内存库（MySQL 模式），
 * Flyway 用同一份迁移脚本建表灌种子数据 —— 测的是真表结构，但和本地 MySQL 零干扰。
 *
 * @TestMethodOrder：这个类故意按顺序执行（流程环环相扣），
 * 所以每步都把结果存进静态变量传给下一步。
 * 一般原则「测试之间互不依赖」在这里让位于「测试一条真实业务链路」——
 * 两种风格都合理，知道为什么破例比死记规则重要。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper om = new ObjectMapper();

    // 链路各步骤的接力棒
    private static String token;
    private static long orderId;

    @Test
    @Order(1)
    void 积分榜是公开接口_不带token也能查() throws Exception {
        MvcResult result = mockMvc.perform(get("/standings"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        assertEquals(20, body.get("data").size(), "种子数据应该是 20 支球队");
        // 验证排序规则：第一名积分 27（种子数据里的球队1）
        assertEquals(27, body.get("data").get(0).get("points").asInt());
        assertEquals(1, body.get("data").get(0).get("rank").asInt());
    }

    @Test
    @Order(2)
    void 注册成功_直接返回token() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester01\",\"password\":\"123456\",\"nickname\":\"测试员\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        token = body.get("data").get("token").asText();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT 应该是三段式结构");
    }

    @Test
    @Order(3)
    void 重复注册同一个用户名_返回2001() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester01\",\"password\":\"654321\"}"))
                .andExpect(status().isOk())   // 工程手册裁定：业务失败 HTTP 仍是 200
                .andReturn();
        assertEquals(2001, om.readTree(result.getResponse().getContentAsString()).get("code").asInt());
    }

    @Test
    @Order(4)
    void 错误密码登录_返回2002且不区分是用户不存在还是密码错() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester01\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(2002, body.get("code").asInt());
        assertEquals("用户名或密码错误", body.get("message").asText());
    }

    @Test
    @Order(5)
    void 不带token访问受保护接口_返回1101() throws Exception {
        MvcResult result = mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1101, om.readTree(result.getResponse().getContentAsString()).get("code").asInt());
    }

    @Test
    @Order(6)
    void 带token能查到自己的资料() throws Exception {
        MvcResult result = mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        assertEquals("tester01", body.get("data").get("username").asText());
        assertEquals("测试员", body.get("data").get("nickname").asText());
    }

    @Test
    @Order(7)
    void 下单_金额由后端重算() throws Exception {
        // matchId=1 是种子数据里 29900 分的比赛，买 2 张 = 59800 分。
        // 如果后端信了前端传的金额，这个断言就会失败 —— 这条用例就是「后端重算」的哨兵
        MvcResult result = mockMvc.perform(post("/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":1,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals(0, body.get("code").asInt());
        assertEquals(29900, body.get("data").get("unitPrice").asInt());
        assertEquals(59800, body.get("data").get("totalAmount").asInt());
        assertEquals("pending", body.get("data").get("status").asText());
        orderId = body.get("data").get("id").asLong();
    }

    @Test
    @Order(8)
    void 下单参数不合法_返回1001() throws Exception {
        MvcResult result = mockMvc.perform(post("/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchId\":1,\"quantity\":99}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1001, om.readTree(result.getResponse().getContentAsString()).get("code").asInt());
    }

    @Test
    @Order(9)
    void 支付后_状态机推进到paid() throws Exception {
        mockMvc.perform(post("/order/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/order/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        assertEquals("paid", body.get("data").get("list").get(0).get("status").asText());
    }

    @Test
    @Order(10)
    void 已支付的订单不能取消_状态机守卫返回3002() throws Exception {
        MvcResult result = mockMvc.perform(post("/order/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(3002, om.readTree(result.getResponse().getContentAsString()).get("code").asInt());
    }
}
