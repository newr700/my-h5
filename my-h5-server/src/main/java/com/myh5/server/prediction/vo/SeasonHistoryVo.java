package com.myh5.server.prediction.vo;

/**
 * 历届战绩一行（VO）—— 页面5 上方表格。
 *
 * edition 与 seasonLabel 都给：
 * - edition（第 33 届）是官方口径；
 * - seasonLabel（2024-25）是人看球时习惯的说法。
 * 换算关系（起始年份 + 1 拼出 「2024-25」）在 Service 里做一次，
 * 而不是让前端拿到 2024 自己拼 —— 展示格式属于契约，应该和后端一起改。
 */
public record SeasonHistoryVo(
        int edition,
        int seasonYear,
        String seasonLabel,
        TeamBriefVo champion,
        TeamBriefVo runnerUp,
        TeamBriefVo third
) {
}
