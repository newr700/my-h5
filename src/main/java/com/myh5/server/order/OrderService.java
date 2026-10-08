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
    private final AuditLogService auditLogService;

    public OrderService(OrderMapper orderMapper, MatchInfoMapper matchInfoMapper, AuditLogService auditLogService) {
        this.orderMapper = orderMapper;
        this.matchInfoMapper = matchInfoMapper;
        this.auditLogService = auditLogService;
    }

    /**
     * 创建订单（PRD 5.2 写操作闭环的核心）。
     * 流程：幂等预检 → 查比赛 → 【V2 扣库存】 → 后端算钱 → 落库。
     *
     * ── V2 Step2：幂等（防重复下单）─────────────────────
     * 这一版在「扣库存之前」先按 (userId, requestId) 查是否已存在订单：
     *   · 命中 → 直接返回【原订单】，并且【不重复扣库存】。
     *     覆盖「用户连点」「前端重试」「网络抖动重发」绝大多数重复场景。
     *   · 未命中 → 正常走下单；唯一索引 (user_id, request_id) 作为并发兜底，
     *     保证两个相同 requestId 绝不会建出两笔订单（撞索引时回滚并抛 3004 让前端重试）。
     * requestId 由前端在「一笔下单意图」开始时生成并持久化（失败保留、成功清空），
     * 见前端 api/order.ts 的 pendingRequestId 机制。这是「客户端生成幂等号 + 服务端唯一约束」
     * 的经典组合，比服务端用 session 记订单更可靠（跨标签页 / 刷新后仍有效）。
     *
     * ── V2 Step3：审计（关键操作留痕）─────────────────────
     * 下单的【成功】与【失败】两条路径都记一笔审计：
     *   · 成功（含幂等命中返回原单）→ result=1，记 status=pending、request_id；
     *   · 失败（比赛下架 1002 / 库存不足 3003 / 并发冲突 3004）→ result=0，记 error_code。
     * 审计用 REQUIRES_NEW 独立提交，所以即使本方法因异常整体回滚，
     * 「这次失败的下单尝试」依然会留在 audit_log（详见 AuditLogService 类注释）。
     *
     * @Transactional 的意义（V2 后更关键）：方法里既有「UPDATE 库存」又有「INSERT 订单」，
     * 没事务时任一环节出错会出现「库存扣了但订单没建成」（用户没买到，票还少了）。
     * 有事务，抛异常两步一起回滚 —— 要么都成，要么都不成。
     */
    @Transactional
    public OrderVo create(CreateOrderRequest req) {
        Long userId = AuthContext.requireUserId();
        String requestId = req.requestId();

        // ── 幂等主路径：同一 requestId 已经下过单，直接返还原订单 ──────
        // 注意返回原订单前【不要】扣库存 —— 否则「重复提交」会变成「重复扣票」，
        // 那比不幂等还糟（用户没多买，库存却凭空少了）。
        OrderEntity existing = orderMapper.selectByUserAndRequest(userId, requestId);
        if (existing != null) {
            // 幂等命中也是一次「成功的下单结果」，照常留痕（便于串联排查）
            auditLogService.record(userId, "CREATE_ORDER", existing.getId(), null, "pending",
                    true, null, requestId, "幂等命中，返回原订单");
            return toVo(existing, matchInfoMapper.selectById(existing.getMatchId()));
        }

        try {
            MatchInfoEntity match = matchInfoMapper.selectById(req.matchId());
            if (match == null || !"on_sale".equals(match.getStatus())) {
                throw new BizException(ErrorCodes.NOT_FOUND, "比赛不存在或已下架");
            }

            // ── V2：先把票占下来（并发安全，见 deductStock 注释）────────
            int affected = matchInfoMapper.deductStock(match.getId(), req.quantity());
            if (affected == 0) {
                // 抛异常 → @Transactional 回滚 → 本次没扣到票也不会留下任何半成品记录。
                throw new BizException(ErrorCodes.STOCK_NOT_ENOUGH,
                        "票不够了（可能刚被别人买走），请刷新后重试");
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
            order.setRequestId(requestId); // V2 Step2：带上幂等号，唯一索引据此防重复
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orderMapper.insert(order);

            // 成功留痕：动作 / 对象 / 状态迁移 / 幂等号 全部落到审计表（独立事务提交）
            auditLogService.record(userId, "CREATE_ORDER", order.getId(), null, "pending",
                    true, null, requestId, "quantity=" + req.quantity());
            return toVo(order, match);

        } catch (DuplicateKeyException e) {
            // 并发幂等：两个相同 requestId 同到，都预检无、都扣了库存；
            // 一个 insert 成功，另一个撞 (user_id, request_id) 唯一索引。
            // 这里必须让事务回滚 —— @Transactional 会因这个重抛的异常整体回滚，
            // 否则「扣了库存却没建成订单」会凭空少票。
            // 回滚后前端用同一 requestId 重试，会命中上面的预检返回原订单。
            // 用 REQUIRES_NEW 的审计把「这次失败」也记下来，再原样抛出。
            auditLogService.record(userId, "CREATE_ORDER", null, null, null,
                    false, ErrorCodes.DUPLICATE_SUBMIT, requestId, "并发唯一索引冲突");
            throw new BizException(ErrorCodes.DUPLICATE_SUBMIT, "重复提交，请重试");

        } catch (BizException e) {
            // 业务规则不满足（比赛下架 / 库存不足等）：记下失败尝试，再原样抛出，
            // 让外层 @Transactional 回滚；审计记录因 REQUIRES_NEW 已独立提交，不会跟着消失。
            auditLogService.record(userId, "CREATE_ORDER", null, null, null,
                    false, e.getCode(), requestId, e.getMessage());
            throw e;
        }
    }

    /** 实体 → VO（标题由比赛表拼出，比赛下架则降级文案） */
    private OrderVo toVo(OrderEntity order, MatchInfoEntity match) {
        String title = (match == null) ? "已下架的比赛"
                : match.getHomeTeam() + " vs " + match.getAwayTeam();
        return new OrderVo(order.getId(), order.getOrderNo(), title,
                match == null ? null : match.getMatchTime(), order.getQuantity(),
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

    /**
     * 支付（mock：不做真实扣款，只推状态机。接真实支付见 PRD 第 7 章「不做什么」）
     *
     * ── V2 Step3：审计 ── 支付成功记 (pending→paid)；失败（状态机守卫 3002 / 订单不存在 3001）
     * 也记一条 result=0 的审计，便于排查「为什么某笔订单没付成」。
     */
    @Transactional
    public void pay(long orderId) {
        Long userId = AuthContext.requireUserId();
        try {
            OrderEntity order = loadOwnOrder(orderId);
            String before = order.getStatus();
            if (!"pending".equals(before)) {
                throw new BizException(ErrorCodes.ORDER_STATE_INVALID,
                        "订单当前状态不允许支付（状态：" + before + "）");
            }
            order.setStatus("paid");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            auditLogService.record(userId, "PAY", orderId, before, "paid", true, null, null, null);
        } catch (BizException e) {
            auditLogService.record(userId, "PAY", orderId, null, null, false, e.getCode(), null, e.getMessage());
            throw e;
        }
    }

    /**
     * 取消：pending → closed；paid 不能取消（退票是另一个业务流程，PRD 明确不做）
     *
     * ── V2 Step3：审计 ── 取消成功记 (pending→closed)；失败（状态机守卫 3002 / 订单不存在 3001）
     * 也记一条 result=0 的审计。
     */
    @Transactional
    public void cancel(long orderId) {
        Long userId = AuthContext.requireUserId();
        try {
            OrderEntity order = loadOwnOrder(orderId);
            String before = order.getStatus();
            if (!"pending".equals(before)) {
                throw new BizException(ErrorCodes.ORDER_STATE_INVALID,
                        "订单当前状态不允许取消（状态：" + before + "）");
            }
            // 【V2】这一步很重要：把票还给票池。
            // 只在「下单时扣、成功后不管」是不行的 —— 用户反复下单取消几次，
            // 票池就被这些「没成交的占位」耗光了，票一张没卖出去却谁也买不了。
            // 这在概念上和「内存泄漏」是一回事：借了没还。
            //
            // 【为什么只有 cancel 回补，pay 不回补】
            // 支付成功意味着票真的卖出去了，库存占用应该【固化】下来；
            // 只有「交易没走成」（取消/超时关闭）才需要把资源还回去。
            // 这也是为什么 V3 迁移脚本回填库存时只统计 status='paid' 的订单。
            order.setStatus("closed");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            matchInfoMapper.restoreStock(order.getMatchId(), order.getQuantity());
            auditLogService.record(userId, "CANCEL", orderId, before, "closed",
                    true, null, null, "quantity=" + order.getQuantity());
        } catch (BizException e) {
            auditLogService.record(userId, "CANCEL", orderId, null, null, false, e.getCode(), null, e.getMessage());
            throw e;
        }
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
