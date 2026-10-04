package com.myh5.server.order;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myh5.server.auth.AuthContext;
import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import com.myh5.server.common.PageResult;
import com.myh5.server.match.MatchInfoEntity;
import com.myh5.server.match.MatchInfoMapper;
import com.myh5.server.order.dto.CreateOrderRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单服务 —— 本项目业务规则密度最高的类，写操作闭环的「后端一半」。
 *
 * 三条核心规则（每条都对应 PRD 里的一句裁定）：
 * ① 金额后端重算：前端只传 matchId + quantity，单价查库、总价现算；
 * ② 状态机守卫：pay/cancel 只能从 pending 出发，越界抛 3002；
 * ③ 归属校验：只能操作自己的订单，别人的订单和不存在的一样报 3001（防枚举）。
 *
 * @Transactional：写操作的原子性保证。方法里任何一步抛异常，
 * 之前执行的 INSERT/UPDATE 全部回滚 —— 不会出现「订单建了但金额算错留半截」。
 * （技能点：事务 ACID；声明式事务的原理是 AOP 代理，同类自调用会失效——经典面试坑）
 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final MatchInfoMapper matchInfoMapper;

    public OrderService(OrderMapper orderMapper, MatchInfoMapper matchInfoMapper) {
        this.orderMapper = orderMapper;
        this.matchInfoMapper = matchInfoMapper;
    }

    /**
     * 创建订单（PRD 5.2 写操作闭环的核心）。
     * 流程：查比赛 → 后端算钱 → 生成订单号 → 落库。
     */
    @Transactional
    public OrderVo create(CreateOrderRequest req) {
        Long userId = AuthContext.requireUserId();

        MatchInfoEntity match = matchInfoMapper.selectById(req.matchId());
        if (match == null || !"on_sale".equals(match.getStatus())) {
            throw new BizException(ErrorCodes.NOT_FOUND, "比赛不存在或已下架");
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setMatchId(match.getId());
        order.setQuantity(req.quantity());
        // 快照单价：以库里的价格为准，与前端展示无关 —— 哪怕前端页面显示错了，账也是对的
        order.setUnitPrice(match.getUnitPrice());
        order.setTotalAmount(match.getUnitPrice() * req.quantity());
        order.setStatus("pending");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 订单号撞了唯一约束（时间戳+随机数极小概率重复）：重试一次。
            // 为什么不循环重试到成功：连续撞两次说明生成器有 bug，该炸出来而不是硬扛
            order.setOrderNo(generateOrderNo());
            orderMapper.insert(order);
        }

        return new OrderVo(order.getId(), order.getOrderNo(),
                match.getHomeTeam() + " vs " + match.getAwayTeam(),
                match.getMatchTime(), order.getQuantity(),
                order.getUnitPrice(), order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt());
    }

    /** 我的订单（分页，pageNum 从 1 开始，pageSize 上限 50 —— 工程手册 4.3） */
    public PageResult<OrderVo> myOrders(long pageNum, long pageSize) {
        Long userId = AuthContext.requireUserId();
        // 上限截断：pageSize=10000 的恶意请求会变成全表扫描，悄悄拖垮数据库
        long safeSize = Math.min(Math.max(pageSize, 1), 50);
        long safeNum = Math.max(pageNum, 1);

        IPage<OrderDetailRow> page = orderMapper.selectMyOrders(new Page<>(safeNum, safeSize), userId);

        List<OrderVo> vos = page.getRecords().stream()
                .map(row -> new OrderVo(row.getId(), row.getOrderNo(),
                        // LEFT JOIN 的兜底：比赛被删时队名是 null，标题降级为「已下架的比赛」
                        row.getHomeTeam() == null ? "已下架的比赛"
                                : row.getHomeTeam() + " vs " + row.getAwayTeam(),
                        row.getMatchTime(), row.getQuantity(),
                        row.getUnitPrice(), row.getTotalAmount(),
                        row.getStatus(), row.getCreatedAt()))
                .toList();

        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 支付（mock：不做真实扣款，只推状态机。接真实支付见 PRD 第 7 章「不做什么」） */
    @Transactional
    public void pay(long orderId) {
        OrderEntity order = loadOwnOrder(orderId);
        if (!"pending".equals(order.getStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID,
                    "订单当前状态不允许支付（状态：" + order.getStatus() + "）");
        }
        order.setStatus("paid");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 取消：pending → closed；paid 不能取消（退票是另一个业务流程，PRD 明确不做） */
    @Transactional
    public void cancel(long orderId) {
        OrderEntity order = loadOwnOrder(orderId);
        if (!"pending".equals(order.getStatus())) {
            throw new BizException(ErrorCodes.ORDER_STATE_INVALID,
                    "订单当前状态不允许取消（状态：" + order.getStatus() + "）");
        }
        order.setStatus("closed");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 加载订单并校验归属 —— 「不存在」和「不是你的」报同一个 3001。
     * 如果分别报「订单不存在」和「无权查看」，攻击者枚举订单号
     * 就能探测出哪些订单真实存在（信息泄露），混为一体就探测不到。
     */
    private OrderEntity loadOwnOrder(long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        Long userId = AuthContext.requireUserId();
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCodes.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /** 订单号 = MO + 时间戳(14位) + 4位随机数；唯一性最终由 uk_match_order_no 约束兜底 */
    private String generateOrderNo() {
        return "MO" + LocalDateTime.now().format(ORDER_NO_TIME) + RandomUtil.randomNumbers(4);
    }
}
