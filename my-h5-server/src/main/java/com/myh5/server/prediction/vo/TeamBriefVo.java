package com.myh5.server.prediction.vo;

import com.myh5.server.prediction.FootballTeamEntity;

/**
 * 球队简要信息（VO）—— 凡是「要显示队徽 + 队名」的地方都复用它。
 *
 * 出现在：历届战绩表格的三个名次、夺冠次数榜、AI 预测的球队行。
 * 抽出来的价值在于「契约只写一次」：以后加「队徽深色描边」这类字段，
 * 三处展示同时生效，不会漏掉某一处显示不出来。
 *
 * 契约要点（与前端 types/api.ts 的 TeamBrief 一一对应）：
 * - 图片类字段没有图时给空字符串 ''，不给 null（前端靠它决定显示图片还是兜底图形）；
 * - 颜色是 #RRGGBB 字符串，直接就是合法的 CSS 颜色值，前端不用再做映射。
 */
public record TeamBriefVo(
        String teamName,
        String shortName,
        String colorPrimary,
        String colorSecondary,
        String logoUrl
) {

    /** 字典缺行时的兜底配色：灰色中性盾牌，至少不穿帮 */
    private static final String FALLBACK_SHORT = "TBD";
    private static final String FALLBACK_PRIMARY = "#909399";
    private static final String FALLBACK_SECONDARY = "#C0C4CC";

    /**
     * 由球队字典行构造 VO。
     *
     * 【为什么要接一个可能为 null 的 team】
     * 业务表只存队名，颜色靠字典回填。万一某支球队的字典行漏了（新增球队忘了补资料），
     * 直接 NPE 会让整个页面 500 —— 而实际上「缺一行展示资料」远没严重到要让全页崩掉。
     * 所以这里降级成灰色兜底队徽，页面照常显示队名，问题留给日志和数据修复。
     * 这类「局部数据缺失不该升级成整页故障」的判断，是接口健壮性的日常体现。
     */
    public static TeamBriefVo from(FootballTeamEntity team, String teamName) {
        if (team == null) {
            return new TeamBriefVo(teamName, FALLBACK_SHORT, FALLBACK_PRIMARY, FALLBACK_SECONDARY, "");
        }
        return new TeamBriefVo(
                team.getTeamName(),
                team.getShortName(),
                team.getColorPrimary(),
                team.getColorSecondary(),
                // 契约：没图给 ''，不给 null
                team.getLogoUrl() == null ? "" : team.getLogoUrl()
        );
    }
}
