package com.myh5.server.analysis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 权威解析实体（专家观点卡片）—— 对应 expert_analysis 表（V7）。 */
@Data
@TableName("expert_analysis")
public class ExpertAnalysisEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 英文名：草图上「可选用中/英」，所以中英名各存一列，切换语言时不用再请求一次 */
    @TableField("expert_name_en")
    private String expertNameEn;

    @TableField("expert_name_cn")
    private String expertNameCn;

    /** 专家身份/头衔，如「资深英超解说，擅长数据复盘」—— 卡片上姓名下方的标签 */
    @TableField("expert_title")
    private String expertTitle;

    /** 该专家支持的球队，决定这张卡片的配色 */
    private String teamName;

    /** 支持理由，卡片正文 */
    private String reason;

    /** 头像；NULL 走昵称首字兜底 */
    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("sort_order")
    private Integer sortOrder;
}
