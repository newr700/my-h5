package com.myh5.server.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体 —— 对应 match_order 表。
 *
 * 状态机（PRD 5.3 裁定，整个项目最重要的业务规则之一）：
 *
 *            支付 pay()              取消 cancel()
 *   pending ────────────► paid     pending ────────────► closed
 *      │                                                   ▲
 *      └── 只允许从 pending 出发；paid 和 closed 都是「回不去」的状态
 *
 * 每一条边都是一个 if 守卫（见 OrderService），越界操作抛 3002。
 * 为什么状态用字符串而不是数字：'pending' 在日志和数据库里一眼能懂，
 * 1/2/3 三个月后没人记得哪个是哪个 —— 可读性也是健壮性。
 */
@Data
@TableName("match_order")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号（MO+时间戳+随机数），对外暴露用它，不用自增 id */
    private String orderNo;

    /** 幂等号（V2 Step2）：客户端生成的 requestId，配合 (user_id, request_id) 唯一索引防重复下单 */
    @TableField("request_id")
    private String requestId;

    private Long userId;
    private Long matchId;
    private Integer quantity;

    /** 下单时单价快照（分）—— 比赛改价不影响已下订单 */
    private Integer unitPrice;

    /** 总价（分）= 单价 × 数量，后端重算，不信前端传值（PRD 5.2） */
    private Integer totalAmount;

    /** pending / paid / closed */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
