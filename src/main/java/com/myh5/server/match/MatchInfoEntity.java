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

    // ── V2 库存模型（见 db/migration/V3__stock.sql）──────────────
    /** 总票数 —— 只增不常改，用于展示「共 N 张」和给回补库存做上限参照 */
    private Integer totalStock;

    /**
     * 剩余票数 —— 【并发热点列】。
     *
     * 所有抢票请求最终都会挤到这一行上：扣减要给行加锁，所以同一场比赛的
     * 下单请求在数据库层面天然【串行化】了。这既是它的正确性来源，
     * 也是它的性能上限 —— 秒杀场景下这里会先成为瓶颈，
     * 届时的优化方向是「缓存层预扣减 + 异步落库」或「库存分片（把一行拆成多行）」，
     * 那些方案本质上都是想办法绕开单一热点行。MVP 阶段直接用行锁，够用且最可靠。
     */
    private Integer stock;
}
