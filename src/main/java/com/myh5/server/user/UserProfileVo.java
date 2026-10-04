package com.myh5.server.user;

import java.time.LocalDateTime;

/** 个人资料响应：注意依然没有 passwordHash，且多一个用户名用于展示 */
public record UserProfileVo(
        long id,
        String username,
        String nickname,
        LocalDateTime createdAt
) {
}
