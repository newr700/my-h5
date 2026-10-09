package com.myh5.server.prediction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 预测：球队评估实体 —— 对应 team_prediction 表（V7 建表，V11 改为文档口径）。
 *
 * 数据来自文档「AI模型预测」：五个评估维度（0~100）+ 综合得分 + 夺冠概率 + 分析文字。
 * 夺冠概率是【文档直接给定的】，不是加权算出来的 —— 表里就存这个事实值，
 * 与积分榜不存 rank 列是同一个思路：避免「算好的概率」和「原始评分」两处真相打架。
 */
@Data
@TableName("team_prediction")
public class TeamPredictionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;

    // 五个评估维度各 0~100（文档给定），顺序与前端雷达图的顶点一一对应
    private Integer squadScore;       // 阵容实力
    private Integer tacticScore;      // 战术成熟度
    private Integer homeAwayScore;    // 主场优势
    private Integer benchScore;       // 板凳深度
    private Integer injuryRisk;       // 伤病风险（0~100，越高风险越大，语义与其他维相反）

    private Integer overallScore;            // 综合得分（0~100，文档给定）
    private Integer championProbability;     // 夺冠概率百分数（0~100，文档直接给定，非加权算出）
    /** AI 分析文字（文档给定）；NULL 表示暂无 */
    private String analysis;
}
