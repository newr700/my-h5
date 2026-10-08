package com.myh5.server.order;

import java.time.LocalDateTime;

/** 订单 VO：给前端的最终形状（与前端 src/types/api.ts 的 OrderItem 一一对应） */
public record OrderVo(
        long id,
        String orderNo,
        String matchTitle,
        LocalDateTime matchTime,
        int quantity,
        /** 单价（分） */
        int unitPrice,
        /** 总价（分） */
        int totalAmount,
        /** pending / paid / closed */
        String status,
        LocalDateTime createdAt
) {
}
