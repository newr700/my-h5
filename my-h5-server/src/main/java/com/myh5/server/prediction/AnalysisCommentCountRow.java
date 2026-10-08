package com.myh5.server.prediction;

import lombok.Data;

/**
 * 「每条解析有几条评论」的查询结果行模型 —— 只在 Mapper ↔ Service 之间流动。
 * 字段名与 XML 里的列别名逐字对齐（analysisId / commentCount）。
 *
 * 别名为什么不起名叫 count：count 是 SQL 的聚合函数名，
 * 当别名用虽然不是语法错误，但可读性差、换数据库时也容易撞上保留字 —— 换个名字零成本。
 */
@Data
public class AnalysisCommentCountRow {
    private Long analysisId;
    private Integer commentCount;
}
