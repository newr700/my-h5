package com.myh5.server.history;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 历届战绩 Mapper。
 *
 * 【为什么这里用注解而不是 XML】
 * 项目约定「复杂查询手写 XML」，这条 SQL 虽然带 GROUP BY，但结构固定、没有动态条件，
 * 单行注解足够清晰；真到了需要动态拼接（按年份/联赛筛选）时再搬去 XML。
 * 约定是用来省事的，不是用来把简单的写法逼复杂的。
 *
 * 【方言代价】LIMIT 是 MySQL 方言（Oracle 要写 ROWNUM，SQL Server 要写 TOP）。
 * 本项目只支持 MySQL 8 与 H2(MySQL 模式)，两种都认 LIMIT，所以可以接受；
 * 哪天要接第三种数据库，这里就是必须改的点 —— 提前知道代价在哪，比事后踩到强。
 */
@Mapper
public interface SeasonHistoryMapper extends BaseMapper<SeasonHistoryEntity> {

    /**
     * 统计各队夺冠次数（只算 fromYear 年起），按次数倒序取前 limit 名。
     *
     * 结果由【现有数据现算】而不是另建一张统计表：
     * 统计数字与明细数据同源，永远不可能对不上（存两份 = 早晚不一致）。
     * 这也是页面5「英超历届夺冠次数」在追加新赛季后自动更新的原因。
     *
     * 【踩过的坑：接口上写了 @Select 才有 SQL】
     * Mapper 里多表/聚合方法如果既没有注解、也没有对应的 XML 语句，
     * 编译期完全不报错，要到第一次调用才抛
     *   Invalid bound statement (not found): ...SeasonHistoryMapper.selectTitleCounts
     * 因为它披着 Result 外壳变成 {"code":5000}，很容易被当成「服务端抽风」。
     * 记牢这条报错：它几乎总是「SQL 忘了绑」，而不是数据库坏了。
     *
     * 【为什么 ORDER BY 写 COUNT(*) 而不是别名 titleCount】
     * 两种在 MySQL / H2 上都对；写成表达式更保险 —— 个别数据库的 ORDER BY
     * 对别名解析有细微差别，而表达式到处都一样。
     *
     * @param fromYear 起始赛季年份（含），页面口径为 1992（英超元年，即全时期）
     * @param limit    取前几名，页面口径为 10
     */
    @Select("""
            SELECT
                champion_team AS teamName,
                COUNT(*)      AS titleCount
            FROM season_history
            WHERE season_year >= #{fromYear}
            GROUP BY champion_team
            ORDER BY COUNT(*) DESC, champion_team ASC
            LIMIT #{limit}
            """)
    List<ChampionTitleRow> selectTitleCounts(@Param("fromYear") int fromYear, @Param("limit") int limit);
}
