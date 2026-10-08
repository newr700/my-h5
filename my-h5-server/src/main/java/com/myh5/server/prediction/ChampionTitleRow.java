package com.myh5.server.prediction;

import lombok.Data;

/** 「各队夺冠次数」的查询结果行模型 —— GROUP BY 的返回形状。 */
@Data
public class ChampionTitleRow {
    private String teamName;
    private Integer titleCount;
}
