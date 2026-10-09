package com.myh5.server.prediction;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.myh5.server.prediction.vo.SeasonHistoryVo;
import com.myh5.server.prediction.vo.TeamBriefVo;
import com.myh5.server.prediction.vo.TitleCountVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 历史回顾服务（页面5）。
 *
 * 两块数据：
 *   ① 历届战绩表格：season_history 按届数倒序（最新的赛季排最上面，符合阅读习惯）；
 *   ② 夺冠次数榜：由 season_history 现算（GROUP BY champion_team），
 *      不是另建一张统计表 —— 明细与统计同源，永远不可能对不上。
 */
@Service
public class HistoryService {

    /** 统计口径：草图上「历届夺冠次数」—— 全时期（自 1992-93 英超元年起）；只显示前 10 名 */
    private static final int TITLE_FROM_YEAR = 1992;
    private static final int TITLE_TOP_N = 10;

    private final SeasonHistoryMapper seasonHistoryMapper;
    private final TeamDictionary teamDictionary;

    public HistoryService(SeasonHistoryMapper seasonHistoryMapper, TeamDictionary teamDictionary) {
        this.seasonHistoryMapper = seasonHistoryMapper;
        this.teamDictionary = teamDictionary;
    }

    public List<SeasonHistoryVo> listSeasons() {
        Map<String, FootballTeamEntity> teamMap = teamDictionary.asMap();

        // 倒序：用户点开「历史回顾」最想先看到的是刚过去的赛季
        return seasonHistoryMapper.selectList(
                        new QueryWrapper<SeasonHistoryEntity>().orderByDesc("edition"))
                .stream()
                .map(e -> new SeasonHistoryVo(
                        e.getEdition(),
                        e.getSeasonYear(),
                        seasonLabel(e.getSeasonYear()),
                        brief(teamMap, e.getChampionTeam()),
                        brief(teamMap, e.getRunnerUpTeam()),
                        brief(teamMap, e.getThirdTeam()),
                        brief(teamMap, e.getFourthTeam())))
                .toList();
    }

    public List<TitleCountVo> topTitleCounts() {
        Map<String, FootballTeamEntity> teamMap = teamDictionary.asMap();

        return seasonHistoryMapper.selectTitleCounts(TITLE_FROM_YEAR, TITLE_TOP_N).stream()
                .map(row -> new TitleCountVo(
                        brief(teamMap, row.getTeamName()),
                        row.getTitleCount() == null ? 0 : row.getTitleCount()))
                .toList();
    }

    /**
     * 队名 → 球队简要信息。
     *
     * 抽成一个私有方法而不是写三遍，靠的是把「队名 → 字典」这一步也留在内部：
     * 调用处只关心「给我这个队名的展示信息」，不用知道中间还有个字典表。
     */
    private TeamBriefVo brief(Map<String, FootballTeamEntity> teamMap, String teamName) {
        return TeamBriefVo.from(teamMap.get(teamName), teamName);
    }

    /**
     * 赛季展示标签：起始年份 → 「2000-01」。
     *
     * 为什么不让前端拼：这是【展示口径】，属于契约。
     * 以后想显示成「00/01 赛季」或「2000–2001」，改这一处全站生效；
     * 若让每个页面自己拼，就会出现 A 页面 2000-01、B 页面 2000/01 的不一致。
     *
     * year + 1 取后两位：2000 → 2001 → "01"；
     * %02d 保证 2009 → "09" 而不是 "9"（补零这种小事，出一次就够难看的了）。
     */
    private String seasonLabel(int seasonYear) {
        return seasonYear + "-" + String.format("%02d", (seasonYear + 1) % 100);
    }
}
