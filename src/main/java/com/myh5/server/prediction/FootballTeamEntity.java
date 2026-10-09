package com.myh5.server.prediction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 球队资料实体 —— 对应 football_team 表（V7）。
 *
 * 这张表是「字典表」：三个新页面（权威解析 / AI 预测 / 历史回顾）都要用球队的颜色和缩写，
 * 于是把它抽成唯一事实来源。Service 一次性查出来放进 Map，再按队名回填到各页面的 VO 里 ——
 * 见 TeamPredictionService / HistoryService 里的 teamMap 用法。
 *
 * 为什么要走「内存里拼」而不是写三张表 JOIN 球队表的 SQL？
 * 因为这是 20 行的字典数据，一次查询的成本可以忽略；
 * 而写在 SQL 里意味着三处 JOIN + 三处结果集映射，将来加一个字段要改三个地方。
 * 数据量小的时候，把字典装进内存是用确定性换简洁，这笔账很划算 ——
 * 但如果球队表涨到几十万行（比如扩到全球联赛），这个结论立刻反转，必须回到 JOIN。
 */
@Data
@TableName("football_team")
public class FootballTeamEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 中文队名，也是其他表引用球队的业务键（沿用项目既有的「存队名不存 id」约定） */
    private String teamName;

    private String teamNameEn;
    private String shortName;

    /** 主色 #RRGGBB —— 草图上「颜色有对应码，和资料一起出」的落点 */
    @TableField("color_primary")
    private String colorPrimary;

    @TableField("color_secondary")
    private String colorSecondary;

    /** 真队徽；NULL 表示暂无，前端用「双色盾牌 + 缩写」兜底 */
    @TableField("logo_url")
    private String logoUrl;
}
