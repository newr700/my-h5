package com.myh5.server.prediction.vo;

/**
 * 夺冠次数榜一项（VO）—— 页面5 下方的「2000 年以来夺冠次数」。
 *
 * 数据是 season_history 现算出来的（GROUP BY champion_team + COUNT），
 * 不是单独维护的统计字段：明细和统计同源，追加赛季时统计自动更新。
 */
public record TitleCountVo(
        TeamBriefVo team,
        int count
) {
}
