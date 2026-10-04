package com.myh5.server.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 创建订单请求。
 *
 * 注意这里【没有】金额字段 —— 这不是漏了，是故意不收。
 * 金额由后端拿着 matchId 查库里的单价自己算（PRD 5.2：总价后端重算）。
 * 如果收了 amount 字段，哪怕只是「参考」，也是在邀请篡改：
 * Burp Suite 改一个字段，1 万块的票 1 分钱买走 —— 电商系统最经典的越价漏洞。
 */
public record CreateOrderRequest(
        @NotNull(message = "比赛 id 不能为空")
        Long matchId,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "至少买 1 张")
        @Max(value = 10, message = "单笔最多买 10 张")
        Integer quantity
) {
}
