package com.myh5.server.standings;

/**
 * 积分榜条目（VO，给前端看的形状）—— 与前端 src/types/api.ts 的 TeamStanding 一一对应。
 *
 * 契约要点（PRD-F1，一个字都不能改）：
 * - 字段是 win/draw/lose，不是 won/drawn/lost；
 * - 没有 id 字段；没有 goalDiff（净胜球只参与后端排序，不下发）；
 * - logoUrl 没有图时给空字符串 ''，不给 null（前端靠它判断显示图片还是排名圆圈）。
 *
 * 用 record：不可变 + 免样板代码（同 Result）。
 */
public record StandingVo(
        int rank,
        String teamName,
        int played,
        int win,
        int draw,
        int lose,
        int goalsFor,
        int goalsAgainst,
        int points,
        String logoUrl
) {
}
