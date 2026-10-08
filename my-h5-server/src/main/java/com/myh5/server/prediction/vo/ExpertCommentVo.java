package com.myh5.server.prediction.vo;

import java.time.LocalDateTime;

/**
 * 评论（VO）—— 与前端 types/api.ts 的 ExpertComment 一一对应。
 *
 * 契约要点：
 * - userId 可能是 null（种子数据里的演示评论不对应真实账号），
 *   前端靠「userId === 当前用户 id」判断哪条是自己发的，null 自然就不匹配；
 * - userLevel 是发布时的等级快照，页面据此给专家评论加一个身份标记；
 * - createdAt 是 LocalDateTime，由全局 Jackson 配置统一序列化成
 *   "yyyy-MM-dd HH:mm:ss" 字符串（工程手册 4.3：时间一律字符串，东八区），
 *   前端不需要再做时区转换 —— 这正是「统一配置一次，全项目受益」的例子。
 */
public record ExpertCommentVo(
        long id,
        long analysisId,
        Long userId,
        String nickname,
        int userLevel,
        String content,
        LocalDateTime createdAt
) {
}
