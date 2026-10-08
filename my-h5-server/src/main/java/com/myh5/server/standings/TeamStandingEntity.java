package com.myh5.server.standings;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 积分榜实体 —— 对应 team_standing 表。
 * 注意：表里没有 rank 列（V1__init.sql 注释解释了为什么：名次是计算结果不是存储数据）。
 */
@Data
@TableName("team_standing")
public class TeamStandingEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;
    private Integer played;
    private Integer win;
    private Integer draw;
    private Integer lose;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer points;
}
