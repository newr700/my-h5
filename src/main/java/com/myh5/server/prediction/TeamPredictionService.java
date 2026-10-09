package com.myh5.server.prediction;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.myh5.server.prediction.vo.DimScoreVo;
import com.myh5.server.prediction.vo.StarPlayerVo;
import com.myh5.server.prediction.vo.TeamPredictionVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 预测服务（页面4）。
 *
 * ── 这个类最值得讲的一处判断：排序为什么没写在 SQL 里 ─────────────
 * 项目里另一处排序（积分榜）是写进 ORDER BY 的，注释还专门论证了
 * 「过滤和排序推给数据库是铁律」。这里为什么反过来了？
 *
 *   积分榜排的是 points / goals_for 这些【存储列】，数据库能直接排；
 *   夺冠概率是【五个列算出来的值】，SQL 里排它就得把权重表达式整套抄进 ORDER BY：
 *       ORDER BY (history_score*15 + star_score*20 + ...) DESC
 *   于是权重同时存在于 PredictionScoring 和 SQL 字符串里，改一处忘一处 ——
 *   这正是本项目反复强调的「单一事实来源」被破坏。为 20 行数据引入这种维护风险，不值。
 *
 *   所以正确的判断依据不是「排序一定要推给数据库」，
 *   而是「排序键是不是数据库能便宜算出的存储列」。数据量真的涨起来
 *   （比如接入上百个联赛），再考虑把它固化成生成列 + 索引 —— 那时结论会变，
 *   但变的是结论不是原则：原则是「让排序发生在最懂它的地方，且只定义一次」。
 */
@Service
public class TeamPredictionService {

    private final TeamPredictionMapper predictionMapper;
    private final TeamStarPlayerMapper starPlayerMapper;
    private final TeamDictionary teamDictionary;

    public TeamPredictionService(TeamPredictionMapper predictionMapper,
                                 TeamStarPlayerMapper starPlayerMapper,
                                 TeamDictionary teamDictionary) {
        this.predictionMapper = predictionMapper;
        this.starPlayerMapper = starPlayerMapper;
        this.teamDictionary = teamDictionary;
    }

    /**
     * 按夺冠概率倒序返回全部球队。
     *
     * 取数一共 3 次查询（五维评分 / 明星球员 / 球队字典），
     * 而且是「取全量 → 内存里组装」，不是「循环里查库」的 N+1 ——
     * 20 支球队的场景下，3 次往返远优于 1 + 20 + 20 次。
     */
    public List<TeamPredictionVo> listSorted() {
        Map<String, FootballTeamEntity> teamMap = loadTeamMap();
        Map<String, List<StarPlayerVo>> starPlayerMap = loadStarPlayers();

        List<TeamPredictionEntity> sorted = new ArrayList<>(predictionMapper.selectList(null));
        // 概率相同时用队名兜底排序：排序结果必须是【确定的】，
        // 否则两次请求顺序不同，用户会以为数据在跳（这类「不确定的排序」是分页 bug 的常见根源）
        // 夺冠概率由文档直接给定（champion_probability），按它倒序；同分用队名兜底保证确定性
        sorted.sort(Comparator.comparingInt(
                        (TeamPredictionEntity e) -> e.getChampionProbability() == null ? 0 : e.getChampionProbability())
                .reversed()
                .thenComparing(TeamPredictionEntity::getTeamName));

        // rank 与积分榜同一套做法：排好序后名次就是下标 + 1，页面不自己算
        return IntStream.range(0, sorted.size())
                .mapToObj(i -> toVo(sorted.get(i), i + 1, teamMap, starPlayerMap))
                .toList();
    }

    /**
     * 下发给页面「数据说明」区展示的评估维度定义（名称 + 顺序）。
     *
     * 【为什么维度定义要专门开一个接口暴露】
     * 一个「AI 预测」如果只给结论不给维度，用户不知道这个分数从哪五个角度来。
     * 把维度名称与顺序摆出来，页面才能画出对应的雷达图坐标轴、并给出可读的说明。
     * 维度只有 DIMS 一处定义，前端展示的就是后端真实使用的维度，不会漂移。
     *
     * 复用 DimScoreVo 传维度（score 字段本次不再表示权重、恒为 0），
     * 省得为一个纯展示需求再造一个几乎相同的类型。
     */
    public List<DimScoreVo> weights() {
        return PredictionScoring.DIMS.stream()
                .map(d -> new DimScoreVo(d.key(), d.label(), d.weight()))
                .toList();
    }

    /** 一次查回球队字典，装进 Map 供后续按队名回填（20 行，内存里查比 JOIN 便宜得多） */
    Map<String, FootballTeamEntity> loadTeamMap() {
        return teamDictionary.asMap();
    }

    /** 一次查回全部球员再按队名分组，同样是为了避开 N+1 */
    private Map<String, List<StarPlayerVo>> loadStarPlayers() {
        return starPlayerMapper.selectList(new QueryWrapper<TeamStarPlayerEntity>()
                        .orderByAsc("team_name", "sort_order"))
                .stream()
                .collect(Collectors.groupingBy(TeamStarPlayerEntity::getTeamName,
                        Collectors.mapping(p -> new StarPlayerVo(
                                p.getPlayerName(),
                                p.getPosition(),
                                p.getJerseyNumber() == null ? 0 : p.getJerseyNumber(),
                                // 契约：没照片给 ''，前端用球衣号圆牌兜底
                                p.getPhotoUrl() == null ? "" : p.getPhotoUrl()
                        ), Collectors.toList())));
    }

    /** 实体 + 字典 + 球员 → 页面要的 VO，字段差异全部在这一个方法里收口 */
    private TeamPredictionVo toVo(TeamPredictionEntity e, int rank,
                                  Map<String, FootballTeamEntity> teamMap,
                                  Map<String, List<StarPlayerVo>> starPlayerMap) {
        FootballTeamEntity team = teamMap.get(e.getTeamName());

        // 维度顺序直接来自 DIMS —— 前端雷达图照着画，就不会出现「后端改了维度、前端还是旧的」
        List<DimScoreVo> dims = PredictionScoring.DIMS.stream()
                .map(d -> new DimScoreVo(d.key(), d.label(), d.extract().applyAsInt(e)))
                .toList();

        return new TeamPredictionVo(
                rank,
                e.getTeamName(),
                team == null ? "" : team.getTeamNameEn(),
                team == null ? "" : team.getShortName(),
                team == null ? "#909399" : team.getColorPrimary(),
                team == null ? "#C0C4CC" : team.getColorSecondary(),
                team == null || team.getLogoUrl() == null ? "" : team.getLogoUrl(),
                // 夺冠概率直接用文档给定值（不再加权算）
                e.getChampionProbability() == null ? 0 : e.getChampionProbability(),
                dims,
                // 空列表也要给（不能给 null）：前端 v-for 一个 null 会直接报错
                starPlayerMap.getOrDefault(e.getTeamName(), List.of()),
                // 分析文字可能为空串
                e.getAnalysis() == null ? "" : e.getAnalysis()
        );
    }
}
