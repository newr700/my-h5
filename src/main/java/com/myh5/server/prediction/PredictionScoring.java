package com.myh5.server.prediction;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 夺冠概率预测模型 —— 草图上那句「需要写预测算法」的落点。
 *
 * ══════════════════════════════════════════════════════════
 *  模型：六维加权评分
 * ══════════════════════════════════════════════════════════
 *  每个维度给 0~100 分，乘上各自的权重再求和，就是「夺冠概率指数」（0~100）。
 *
 *      概率 = Σ (维度分 × 权重) / 100
 *
 *  权重分配与理由：
 *    阵容实力   20%  赛季是 38 轮的消耗战，板凳深度比一场爆发更重要
 *    明星球员   20%  能一己之力改变比赛结果的人，是积分的直接来源
 *    战术分析   20%  打法的先进程度决定上限，也决定对强队时的下限
 *    历史夺冠次数 15%  冠军经验有真实价值，但只占一小部分（过去不等于未来）
 *    对位优势   15%  联赛内部相性差异，解释「纸面更强却年年丢分」
 *    主客优势   10%  主客场差异在现代足球里被逐渐拉平，给最低权重
 *
 * ══════════════════════════════════════════════════════════
 *  三个刻意的工程决定
 * ══════════════════════════════════════════════════════════
 *  ① 【全部用整数运算，不碰浮点】
 *     权重写成 15 / 20 / 10 这样的整数百分比，总和恰好 100。
 *     若写成 0.15 / 0.20，求和会引入二进制浮点误差：
 *     一个本该 71.5 的结果可能算成 71.49999999999999，四舍五入后变成 71 ——
 *     于是「同一份数据，换台机器结果差 1」，而且极难复现。
 *     整数运算下同一份数据永远得到同一个数，这是可验证性的前提。
 *     （技能点：为什么金额、评分这类东西不能用 double —— 同一个坑，换个场景）
 *
 *  ② 【权重表就是这个类的 DIMS 列表，只有一处】
 *     六个维度的键、显示名、权重、取值方式全部集中在 DIMS 里。
 *     概率计算、雷达图数据、算法说明接口三个地方都从它派生 ——
 *     将来把「对位优势」换成「伤病情况」，只改这一个列表，
 *     页面上的六边形、权重说明、概率数值会同步变化。
 *     反过来（维度定义散落在 Service、前端、SQL 三处）是必然出错的设计。
 *
 *  ③ 【模型是纯函数，没有任何依赖】
 *     不注入 Mapper、不读配置、不碰时间 —— 输入一个实体，输出一个整数。
 *     所以它可以被单元测试直接调用（见 PredictionScoringTest），
 *     不用起 Spring、不用连数据库。把算法从框架里摘出来，是让它可被验证的第一步。
 */
public final class PredictionScoring {

    /** 六个维度的定义：键 / 显示名 / 权重（整数百分比） / 取值方式 */
    public record DimDef(String key, String label, int weight, ToIntFunction<TeamPredictionEntity> extract) {
    }

    /** 权重总和。刻意用 100，这样 概率 = 加权和 / 100 是一次整数除法加一次四舍五入 */
    public static final int WEIGHT_TOTAL = 100;

    /**
     * 维度定义表 —— 顺序就是雷达图六个顶点的顺序（从正上方顺时针排列）。
     * 顺序有意义，别随手调换：前端是照着这个顺序画六边形的。
     */
    public static final List<DimDef> DIMS = List.of(
            new DimDef("history", "历史夺冠次数", 15, e -> clamp(e.getHistoryScore())),
            new DimDef("star", "明星球员", 20, e -> clamp(e.getStarScore())),
            new DimDef("homeAway", "主客优势", 10, e -> clamp(e.getHomeAwayScore())),
            new DimDef("tactic", "战术分析", 20, e -> clamp(e.getTacticScore())),
            new DimDef("matchup", "对位优势", 15, e -> clamp(e.getMatchupScore())),
            new DimDef("squad", "阵容实力", 20, e -> clamp(e.getSquadScore()))
    );

    private PredictionScoring() {
        // 工具类不许被实例化
    }

    /**
     * 算一支球队的夺冠概率指数（0~100 的整数）。
     *
     * 加权和用 long 累加：六个维度最大 100 × 权重最大 20 = 2000，六项合计最多 12000，
     * int 装得下，但用 long 是防御性写法 —— 以后加入更多维度或改权重量级时不用回头改类型。
     * 乘法在加法之前完成，全程整数，不存在精度丢失。
     */
    public static int winProbability(TeamPredictionEntity team) {
        long weighted = 0;
        for (DimDef dim : DIMS) {
            weighted += (long) dim.extract().applyAsInt(team) * dim.weight();
        }
        // 整数四舍五入：加半个除数再整除。等价于 Math.round，但不引入 double
        return (int) ((weighted + WEIGHT_TOTAL / 2) / WEIGHT_TOTAL);
    }

    /**
     * 把分数夹到 0~100。
     *
     * 数据库里没有 CHECK 约束（H2 与 MySQL 对 CHECK 的支持口径不完全一致，
     * 为了一份迁移脚本两边都能跑，约束交给代码守）。
     * 于是这里必须防御：万一有人手工插了一条 9999，概率会算出天文数字，
     * 页面上的进度条直接溢出。夹一下，最坏情况是这一维显示满分，不会污染整页。
     */
    private static int clamp(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }
}
