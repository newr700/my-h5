package com.myh5.server.user;

import java.time.LocalDateTime;

/**
 * 个人资料响应。
 *
 * 契约要点：注意依然【没有】passwordHash —— 凡是 VO 都不带敏感字段，
 * 这是「Entity 与 VO 分家」的核心价值：不是形式主义，而是防止某天有人
 * 图省事直接把 Entity 返给前端，把密码哈希一起送出去。
 *
 * userLevel（V7 新增）是刻意下发的：
 * 前端要据此决定「评论框是显示输入框还是显示『仅行业专家可评论』的提示」。
 * 这属于「前端需要知道才能把界面画对」的数据，不是敏感信息 ——
 * 用户自己是什么等级，本来就该让他知道。
 */
public record UserProfileVo(
        long id,
        String username,
        String nickname,
        LocalDateTime createdAt,
        /** 头像相对路径（如 /uploads/12_xxx.png），null/空表示未设置 */
        String avatarUrl,
        /** 用户等级：1=普通球迷 2=行业专家（可发表评论），见 UserLevels */
        int userLevel
) {
}
