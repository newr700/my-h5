package com.myh5.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（DTO = Data Transfer Object，「从外部世界进门的形状」）。
 *
 * ── 为什么入参要单独建类而不是直接用 UserEntity ──────────────
 * Entity 是「数据库表的形状」，里面有 id、created_at、password_hash。
 * 如果拿它当入参，客户端就能传 id 和 createdAt 进来 —— 等于把数据库内部结构
 * 暴露给外部随意赋值（这叫 Mass Assignment 漏洞，真实安全事故常客）。
 * DTO 白名单式地只声明「允许你给的字段」，多传的一律丢弃。
 *
 * 校验注解即文档：规则写在这里，knife4j 文档页会自动展示，
 * @Valid 触发后由 GlobalExceptionHandler 统一转 1001。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 32, message = "用户名长度需在 4-32 之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
        String password,

        /** 昵称可空，空则默认用用户名（兜底逻辑在 Service，不在 DTO —— DTO 只管校验形状） */
        @Size(max = 32, message = "昵称最长 32 个字符")
        String nickname
) {
}
