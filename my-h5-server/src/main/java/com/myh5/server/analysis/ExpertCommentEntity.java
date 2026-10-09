package com.myh5.server.analysis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权威解析的评论实体 —— 对应 expert_comment 表（V7）。
 *
 * nickname / userLevel 是【发布时的快照】，不是从 app_user 现查的：
 * 一个人能发这条评论，依据是「发表那一刻他是行业专家」。
 * 事后被降级不该让历史评论变得可疑，事后升级也不该让几个月前发不出去的评论成立 ——
 * 与订单存「下单时单价快照」是同一个原则：记录事实发生那一刻的值。
 */
@Data
@TableName("expert_comment")
public class ExpertCommentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("analysis_id")
    private Long analysisId;

    /** 发表者 id；NULL 表示演示数据（种子数据里的评论不对应任何真实账号） */
    @TableField("user_id")
    private Long userId;

    private String nickname;

    @TableField("user_level")
    private Integer userLevel;

    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
