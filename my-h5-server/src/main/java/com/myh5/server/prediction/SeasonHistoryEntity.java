package com.myh5.server.prediction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 历届战绩实体 —— 对应 season_history 表（V7）。页面5 上方表格的数据源。 */
@Data
@TableName("season_history")
public class SeasonHistoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 届数（1992-93 赛季为第 1 届）与赛季起始年份两个口径都存 —— 都是给用户看的字段 */
    private Integer edition;

    @TableField("season_year")
    private Integer seasonYear;

    @TableField("champion_team")
    private String championTeam;

    @TableField("runner_up_team")
    private String runnerUpTeam;

    @TableField("third_team")
    private String thirdTeam;
}
