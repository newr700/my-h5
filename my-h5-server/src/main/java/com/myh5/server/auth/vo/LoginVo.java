package com.myh5.server.auth.vo;

/**
 * 登录/注册成功的响应（VO = View Object，「出门给前端看的形状」）。
 * 注意没有 passwordHash —— Entity 里的敏感字段在 VO 层被天然过滤，
 * 这就是「Entity 绝不直接返回给前端」的第二层保险（第一层是 DTO，见 RegisterRequest）。
 */
public record LoginVo(
        String token,
        long id,
        String username,
        String nickname
) {
}
