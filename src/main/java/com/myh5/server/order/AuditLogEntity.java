package com.myh5.server.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体 —— 对应 audit_log 表（V5 迁移新增）。
 *
 * 与 OrderEntity 不同，这里【不】写任何业务方法，纯粹是「一条日志」的载体。
 * 字段全部驼峰命名，由 MyBatis-Plus 默认的 map-underscore-to-camel-case
 * 自动映射到下划线列名（user_id → userId，before_status → beforeStatus …）。
 */
@Data
@TableName("audit_log")
public class AuditLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人（AuthContext 拿到的当前用户） */
    private Long userId;

    /** 动作枚举：CREATE_ORDER / PAY / CANCEL */
    private String action;

    /** 对象类型，当前固定 ORDER（以后可扩展为 USER 等） */
    private String targetType;

    /** 对象 id，如下单记录 order_id；尚不可知时为 NULL */
    private Long targetId;

    /** 操作前状态；无（如新建）为 NULL */
    private String beforeStatus;

    /** 操作后状态；失败未改变为 NULL */
    private String afterStatus;

    /** 1 成功 / 0 失败 */
    private Integer result;

    /** 失败时的业务错误码；成功为 NULL */
    private Integer errorCode;

    /** 关联幂等号（与 V4 打通） */
    private String requestId;

    /** 自由备注：数量 / 失败原因等 */
    private String detail;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
