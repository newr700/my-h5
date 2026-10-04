package com.myh5.server.match;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 比赛实体 —— 对应 match_info 表。球票业务里它扮演「商品」的角色 */
@Data
@TableName("match_info")
public class MatchInfoEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchTime;

    /** 单价（分）。金额字段全程 int，从数据库到前端 JSON 不经过任何浮点 */
    private Integer unitPrice;

    /** on_sale=在售 off_sale=下架 */
    private String status;
}
