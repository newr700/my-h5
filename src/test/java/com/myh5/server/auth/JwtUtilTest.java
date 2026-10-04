package com.myh5.server.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtUtil 单元测试 —— 不启动 Spring、不连数据库，纯 Java 对象直接 new 出来测。
 *
 * ── 什么样的代码适合单测 ─────────────────────────────────
 * 输入确定、输出确定、不依赖外部世界的逻辑（签发/验签、金额计算、状态机）。
 * 这类测试跑一个只要几毫秒，值得多写；
 * 依赖 Spring 上下文的集成测试跑一个要十几秒，只覆盖主链路（见 ApiFlowIntegrationTest）。
 *
 * （技能点：JUnit5 断言 / 异常断言 assertThrows / 测试命名即文档）
 */
class JwtUtilTest {

    // 密钥 ≥ 32 字节（HS256 的硬性要求，少了 jjwt 直接拒签）
    private final JwtUtil jwtUtil = new JwtUtil("unit-test-secret-key-0123456789abcdef", 7200);

    @Test
    void 签发的token能验出同一个用户id() {
        String token = jwtUtil.generate(42L);
        assertEquals(42L, jwtUtil.parseUserId(token));
    }

    @Test
    void 用别的密钥验签必须失败() {
        String token = jwtUtil.generate(42L);
        JwtUtil attacker = new JwtUtil("attacker-secret-key-9876543210fedcba", 7200);
        // 签名对不上 = 伪造的 token，必须抛异常而不是悄悄放行
        assertThrows(JwtException.class, () -> attacker.parseUserId(token));
    }

    @Test
    void 过期的token抛ExpiredJwtException() {
        // 有效期 -1 秒：签出来就已经过期 —— 测试不用真的等 2 小时
        JwtUtil expired = new JwtUtil("unit-test-secret-key-0123456789abcdef", -1);
        String token = expired.generate(42L);
        assertThrows(ExpiredJwtException.class, () -> expired.parseUserId(token));
    }

    @Test
    void 乱七八糟的字符串不是token() {
        assertThrows(Exception.class, () -> jwtUtil.parseUserId("not.a.token"));
    }
}
