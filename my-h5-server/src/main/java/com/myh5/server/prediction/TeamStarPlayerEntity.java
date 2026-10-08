package com.myh5.server.prediction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 明星球员实体 —— 对应 team_star_player 表（V7）。只给头部球队配 1~2 人，其余留空。 */
@Data
@TableName("team_star_player")
public class TeamStarPlayerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String teamName;
    private String playerName;
    private String position;

    @TableField("jersey_number")
    private Integer jerseyNumber;

    /** 球员照片；NULL 时前端用「球衣号 + 姓名首字」的圆牌兜底（与头像同一套降级思路） */
    @TableField("photo_url")
    private String photoUrl;

    @TableField("sort_order")
    private Integer sortOrder;
}
