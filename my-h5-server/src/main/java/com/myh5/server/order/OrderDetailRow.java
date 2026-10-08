package com.myh5.server.order;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 「我的订单」查询结果的行模型 —— 联表查询的返回形状。
 * 它既不是 Entity（跨了两张表），也还不是 VO（字段名待翻译），
 * 是 SQL 结果集的忠实镜像，只在 Mapper ↔ Service 之间流动。
 */
@Data
public class OrderDetailRow {
    private Long id;
    private String orderNo;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchTime;
    private Integer quantity;
    private Integer unitPrice;
    private Integer totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
