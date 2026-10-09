package com.myh5.server.analysis.vo;

/**
 * 权威解析条目（VO）—— 与前端 types/api.ts 的 ExpertAnalysis 一一对应。
 *
 * 契约要点：
 * - nameEn / nameCn 同时下发，页面上「中/英」切换是纯前端行为，不用二次请求；
 * - 球队的颜色与缩写一并带来（草图上「这个模板可以上色，用球队颜色来标记」），
 *   前端拿 colorPrimary 直接当 CSS 值用；
 * - commentCount 由批量聚合查询得到，用来在「评论」按钮上显示条数；
 * - avatarUrl 没图给 ''，前端用姓名首字兜底。
 */
public record ExpertAnalysisVo(
        long id,
        String nameEn,
        String nameCn,
        String title,
        String teamName,
        String teamNameEn,
        String shortName,
        String colorPrimary,
        String colorSecondary,
        String logoUrl,
        String reason,
        String avatarUrl,
        int commentCount
) {
}
