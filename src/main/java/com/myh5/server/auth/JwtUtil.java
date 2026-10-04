package com.myh5.server.auth;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具 —— 令牌的「签发」与「验签」都收敛在这一个类里。
 *
 * ── JWT 是什么（一句话版本）────────────────────────────────
 * 一张「带防伪签名的身份卡片」：服务器用只有自己知道的密钥，
 * 把「用户 id + 过期时间」签名后发给客户端；之后客户端每次请求都带着它，
 * 服务器验签通过就相信卡片里的内容 —— 整个过程服务器不用存任何会话，
 * 这就是「无状态鉴权」（对比：session 方案要服务器记一本账）。
 *
 * ── 结构：header.payload.signature，三段 base64，点号分隔 ──
 * 前两段只是 base64 编码，任何人都能解开看内容 ——
 * 所以 JWT 里【绝不放密码等敏感信息】；安全性全在第三段签名上：
 * 没有密钥就伪造不出合法签名，改一个字符验签就失败。
 *
 * （技能点：JWT 原理 / HS256 对称签名 / 为什么 JWT 能防伪但不能保密）
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expire-seconds}") long expireSeconds) {
        // HS256 要求密钥 ≥ 32 字节，不足时 Keys 会直接抛异常 ——
        // 启动即失败，好过运行时才在签发第一个 token 时崩（fail fast 原则）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    /**
     * 登录成功后签发令牌。
     * subject 放用户 id（字符串形式是 JWT 规范的约定），不放用户名 ——
     * id 永不变，用户名将来可能支持修改。
     */
    public String generate(long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                // signWith(key) 会根据密钥长度自动选最强的 HS 算法（0.12+ 的新 API；
                // 老教程里的 signWith(SignatureAlgorithm.HS256, key) 已废弃）
                .signWith(key)
                .compact();
    }

    /**
     * 验签并取出用户 id。
     *
     * @return 用户 id
     * @throws io.jsonwebtoken.ExpiredJwtException 令牌过期（拦截器据此返回 1102）
     * @throws JwtException 签名不对 / 格式非法（拦截器据此返回 1101）
     *
     * 故意让异常往外抛而不是在这里 catch 返回 null：
     * 「过期」和「伪造」对前端是两种不同的处理（跳登录 vs 提示重新登录），
     * 吞掉异常就丢失了区分它们的能力。
     */
    public long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)   // 验签 + 校验过期时间，一步完成
                .getPayload();
        String subject = claims.getSubject();
        if (StrUtil.isBlank(subject)) {
            throw new JwtException("token 缺少 subject");
        }
        return Long.parseLong(subject);
    }
}
