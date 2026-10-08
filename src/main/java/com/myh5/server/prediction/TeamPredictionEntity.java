package com.myh5.server.prediction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 预测：球队六维评分实体 —— 对应 team_prediction 表（V7）。
 *
 * ⚠️ 注意这里【没有】winProbability 字段，这是刻意的：
 * 夺冠概率是六维分数经加权模型算出来的结果，不是存储数据（理由与积分榜不存 rank 列一致）。
 * 模型住在 PredictionScoring 里，表只存原始事实 —— 权重一改，全表概率自动跟着变。
 */
@Data
@TableName("team_prediction")
public class TeamPredictionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;

    // 六个维度各 0~100，顺序与前端雷达图的六个顶点一一对应
    private Integer historyScore;    // 历史夺冠次数
    private Integer starScore;       // 明星球员
    private Integer homeAwayScore;   // 主客优势
    private Integer tacticScore;     // 战术分析
    private Integer matchupScore;    // 对位优势
    private Integer squadScore;      // 阵容实力
}
