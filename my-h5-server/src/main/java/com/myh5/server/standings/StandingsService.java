package com.myh5.server.standings;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 积分榜服务（PRD-F1）。
 *
 * 排序规则（PRD 裁定，一字不差）：积分 → 净胜球 → 进球 → 队名拼音。
 * 排序放在 SQL 里做（ORDER BY），不取回 Java 里排 ——
 * 数据量大了以后，「把过滤和排序推给数据库」是铁律：
 * 数据库为排序建了索引、做了几十年优化；取回内存排既慢又可能内存溢出。
 */
@Service
public class StandingsService {

    private final StandingsMapper standingsMapper;

    public StandingsService(StandingsMapper standingsMapper) {
        this.standingsMapper = standingsMapper;
    }

    public List<StandingVo> listSorted() {
        // 这里用 QueryWrapper（列名字符串）而不是 LambdaQueryWrapper（方法引用）：
        // 排序键里有「净胜球」这个计算列 (goals_for - goals_against)，
        // Lambda 变体的 orderByDesc 只接受 SFunction，表达不了 SQL 表达式 ——
        // 这是 MP 类型安全的边界，越界时退回字符串是【有意识的交易】，不是偷懒。
        // 代价：列名写错编译器查不出来，运行时才报错 ——
        // 由集成测试兜底（ApiFlowIntegrationTest 第 1 条用例验证了排序结果）。
        List<TeamStandingEntity> entities = standingsMapper.selectList(
                new QueryWrapper<TeamStandingEntity>()
                        .orderByDesc("points", "(goals_for - goals_against)", "goals_for")
                        .orderByAsc("team_name")
        );

        // rank 在这里生成：排好序后名次就是下标 + 1。
        // IntStream 带下标遍历 —— 等价于 C 里的 for (i=0; i<n; i++)，但能用上流式转换
        return IntStream.range(0, entities.size())
                .mapToObj(i -> toVo(entities.get(i), i + 1))
                .toList();
    }

    /** Entity → VO：内部模型翻译成契约模型，字段差异都在这一个方法里收口 */
    private StandingVo toVo(TeamStandingEntity e, int rank) {
        // logoUrl 契约要求「没图给空字符串，不给 null」（前端靠它决定显示图片还是排名圆圈）
        return new StandingVo(rank, e.getTeamName(), e.getPlayed(), e.getWin(), e.getDraw(),
                e.getLose(), e.getGoalsFor(), e.getGoalsAgainst(), e.getPoints(), "");
    }
}
