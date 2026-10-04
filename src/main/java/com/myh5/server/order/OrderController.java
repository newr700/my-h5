package com.myh5.server.order;

import com.myh5.server.common.PageResult;
import com.myh5.server.common.Result;
import com.myh5.server.order.dto.CreateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口（PRD 5.2 写操作闭环）。/order/** 全部在 AuthInterceptor 保护名单里。
 *
 * URL 设计风格：动作用路径段表达（/order/{id}/pay），不混用 REST 动词强迫症。
 * 团队约定 > 理论优雅 —— 全项目统一一种风格，新人不用猜。
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单", description = "只需传 matchId + quantity，金额由后端重算")
    @PostMapping("/create")
    public Result<OrderVo> create(@Valid @RequestBody CreateOrderRequest req) {
        return Result.ok(orderService.create(req));
    }

    @Operation(summary = "我的订单列表", description = "分页，pageNum 从 1 开始，pageSize 上限 50")
    @GetMapping("/list")
    public Result<PageResult<OrderVo>> myOrders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(orderService.myOrders(pageNum, pageSize));
    }

    @Operation(summary = "支付订单（mock）", description = "pending → paid，不处理真实扣款")
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable long id) {
        orderService.pay(id);
        return Result.ok(null);
    }

    @Operation(summary = "取消订单", description = "pending → closed；paid 订单不可取消")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable long id) {
        orderService.cancel(id);
        return Result.ok(null);
    }
}
