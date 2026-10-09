package com.myh5.server.prediction.vo;

import java.util.List;

/**
 * AI 预测条目（VO）—— 与前端 types/api.ts 的 TeamPrediction 一一对应。
 *
 * 契约要点：
 * - rank 由后端生成（列表已按夺冠概率倒序，rank 就是下标 + 1），前端直接显示，不自己算；
 * - winProbability 是文档【直接给定】的夺冠概率百分数（0~100），不是数据库算出来的；
 * - dims 的顺序 = 雷达图五个顶点的顺时针顺序，页面照顺序画即可；
 * - analysis 是文档给定的 AI 分析文字，可能为空串；
 * - starPlayers 可能是空数组（只有头部球队配了球员），页面必须能优雅显示空态。
 */
public record TeamPredictionVo(
        int rank,
        String teamName,
        String teamNameEn,
        String shortName,
        String colorPrimary,
        String colorSecondary,
        String logoUrl,
        int winProbability,
        List<DimScoreVo> dims,
        List<StarPlayerVo> starPlayers,
        String analysis
) {
}
