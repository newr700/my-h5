package com.myh5.server.match;

import java.time.LocalDateTime;

/**
 * 比赛 VO。matchTitle 是后端拼好的展示标题（"球队1 vs 球队2"）——
 * 拼接放后端是因为它是「业务展示规则」，前端只做渲染；
 * 哪天要改成 "【中超】球队1 VS 球队2"，只动后端一处，前端无感。
 */
public record MatchVo(
        long id,
        String matchTitle,
        LocalDateTime matchTime,
        /** 单价（分） */
        int unitPrice
) {
    public static MatchVo from(MatchInfoEntity e) {
        return new MatchVo(e.getId(), e.getHomeTeam() + " vs " + e.getAwayTeam(),
                e.getMatchTime(), e.getUnitPrice());
    }
}
