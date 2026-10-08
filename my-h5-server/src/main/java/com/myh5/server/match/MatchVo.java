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
        int unitPrice,
        // ── V2：库存快照，给前端展示「余 N 张」 ──
        /**
         * 总票数。
         */
        int totalStock,
        /**
         * 剩余票数 —— 这是「某一瞬间的快照」，不是承诺。
         *
         * 【为什么必须把这个危险性写进契约注释】
         * 列表返回的余票，在用户看到数字到点击「提交」之间可能已经变了 ——
         * 网络传输几百毫秒，别人早就把票买走了。
         * 所以前端【只能】用它控制 UI（显示几张、是否置灰按钮），
         * 【绝不能】用它做业务判断（比如「我看着还有 3 张所以肯定能买」）。
         * 真正的把关永远在后端下单那一刻的原子扣减（MatchInfoMapper.deductStock）。
         *
         * 对应工程手册的原则：前端负责展示，后端负责正确。
         */
        int stock
) {
    public static MatchVo from(MatchInfoEntity e) {
        return new MatchVo(e.getId(), e.getHomeTeam() + " vs " + e.getAwayTeam(),
                e.getMatchTime(), e.getUnitPrice(),
                e.getTotalStock(), e.getStock());
    }
}
