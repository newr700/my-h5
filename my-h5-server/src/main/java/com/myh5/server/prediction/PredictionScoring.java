package com.myh5.server.prediction;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * AI 预测的评估维度定义 —— 雷达图顶点与「数据说明」区的单一事实来源。
 *
 * ══════════════════════════════════════════════════════════
 *  数据口径（V11 起）：文档「AI模型预测」给定
 * ══════════════════════════════════════════════════════════
 *  每支球队有五个评估维度（0~100）+ 综合得分 + 夺冠概率 + 分析文字。
 *  夺冠概率由模型【直接给出】（见 team_prediction.champion_probability），
 *  不是这五个维度加权算出来的 —— 所以这里只定义「有哪些维度、叫什么、顺序怎样」，
 *  不再承担「算概率」的职责。
 *
 *  ⚠️ 顺序即雷达图顶点的顺时针顺序（从正上方开始），别随手调换：
 *     前端照着这个顺序画五边形。改维度只改这一个列表，
 *     页面上的雷达图、数据说明区会同步变化。
 *
 *  三个刻意的设计决定（沿用 V7 思路）：
 *  ① 维度定义集中在 DIMS —— 雷达图、数据说明都从它派生，单一事实来源；
 *  ② 模型是纯函数、零依赖 —— 不注入 Mapper、不读配置，将来可单测；
 *  ③ clamp 防御脏数据 —— 数据库无 CHECK 约束，分数越界也不画爆雷达图。
 */
public final class PredictionScoring {

    /** 五个维度的定义：键 / 显示名 / （预留权重位，当前概率不再加权算，恒为 0）/ 取值方式 */
    public record DimDef(String key, String label, int weight, ToIntFunction<TeamPredictionEntity> extract) {
    }

    /**
     * 维度定义表 —— 顺序就是雷达图五个顶点的顺序（从正上方顺时针排列）。
     */
    public static final List<DimDef> DIMS = List.of(
            new DimDef("squad", "阵容实力", 0, e -> clamp(e.getSquadScore())),
            new DimDef("tactic", "战术成熟度", 0, e -> clamp(e.getTacticScore())),
            new DimDef("homeAway", "主场优势", 0, e -> clamp(e.getHomeAwayScore())),
            new DimDef("bench", "板凳深度", 0, e -> clamp(e.getBenchScore())),
            new DimDef("injury", "伤病风险", 0, e -> clamp(e.getInjuryRisk()))
    );

    private PredictionScoring() {
        // 工具类不许被实例化
    }

    /**
     * 把分数夹到 0~100。数据库无 CHECK 约束，脏数据也不该画爆雷达图。
     */
    private static int clamp(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }
}
