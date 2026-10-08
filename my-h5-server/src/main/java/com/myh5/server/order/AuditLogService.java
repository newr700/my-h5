package com.myh5.server.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审计日志服务 —— 关键业务操作的「留痕」入口。
 *
 * ── 为什么 record() 用 REQUIRES_NEW 而不是普通 @Transactional ──
 * 这是审计日志设计里【最关键】的一个决定，值得讲透：
 *
 * 审计要记录的【不只是成功的操作】，更要记录【失败的尝试】
 * （例如下单因库存不足整体回滚 —— 我们恰恰想留下「这次失败的下单」）。
 *
 * 如果 record() 和调用方（OrderService.create/pay/cancel）处在【同一个事务】里，
 * 那么当调用方因为异常整体回滚时，连同这条审计 INSERT 也会一起被撤销，
 * 结果就是「失败的操作什么痕迹都没留下」—— 最该记的反而没记到。
 *
 * REQUIRES_NEW 的含义：调用 record() 时，Spring 会【挂起】外层事务，
 * 开一个【全新独立】的事务去写审计，写完立刻提交；之后再恢复外层事务。
 * 于是无论外层是提交还是回滚，审计记录都已经落库、不会跟着消失。
 *
 * （技能点：声明式事务的传播行为 propagation；REQUIRES_NEW 由独立事务管理器驱动，
 *   要求底层是支持它的事务管理器，本项目默认的 DataSourceTransactionManager 满足。）
 */
@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记一笔审计。
     *
     * @param userId       操作人（AuthContext 拿到的当前用户）
     * @param action       动作枚举：CREATE_ORDER / PAY / CANCEL
     * @param targetId     对象 id（如 order_id）；尚不可知时传 null
     * @param beforeStatus 操作前状态；无（如新建）传 null
     * @param afterStatus  操作后状态；失败未改变传 null
     * @param success      是否成功
     * @param errorCode    失败时的业务错误码；成功传 null
     * @param requestId    关联幂等号；非下单操作传 null
     * @param detail       自由备注（数量 / 失败原因等）；无则 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String action, Long targetId,
                       String beforeStatus, String afterStatus,
                       boolean success, Integer errorCode,
                       String requestId, String detail) {
        AuditLogEntity e = new AuditLogEntity();
        e.setUserId(userId);
        e.setAction(action);
        e.setTargetType("ORDER");
        e.setTargetId(targetId);
        e.setBeforeStatus(beforeStatus);
        e.setAfterStatus(afterStatus);
        e.setResult(success ? 1 : 0);
        e.setErrorCode(errorCode);
        e.setRequestId(requestId);
        e.setDetail(detail);
        e.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(e);
    }
}
