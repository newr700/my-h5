package com.myh5.server.common;

/**
 * 错误码常量 —— 《工程实施手册》4.2 错误码表的代码镜像。
 *
 * ── 为什么错误码要收成常量类而不是散落各处的魔法数字 ──
 * 写成 throw new BizException(3002, "...") 当时很爽，三个月后没人记得
 * 3002 是什么；而且两处对同一个码给出两种含义时，前端拦截器会精神分裂。
 * 收敛到这里后：① 新增错误码必须改这个文件 = 强制「先登记」；
 * ② IDE 的「查找引用」能列出每个码的所有使用点。
 *
 * 改这张表 = 改契约：前端 request.ts 对 1101/1102 有特殊处理（跳登录），
 * 删改这两个码之前先看前端代码。
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // 常量类不许被实例化 —— private 构造器是这个意图的唯一表达方式
    }

    /** 参数校验失败（@Valid 拦截到的非法入参） */
    public static final int PARAM_INVALID = 1001;
    /** 资源不存在（查无此 id） */
    public static final int NOT_FOUND = 1002;
    /** 未登录 / token 缺失或非法 */
    public static final int UNAUTHORIZED = 1101;
    /** token 已过期（前端收到后跳登录页） */
    public static final int TOKEN_EXPIRED = 1102;

    /** 注册：用户名已存在 */
    public static final int USERNAME_EXISTS = 2001;
    /** 登录：用户名或密码错误（必须模糊，不区分哪种错 —— 防枚举，PRD 裁定） */
    public static final int BAD_CREDENTIALS = 2002;

    /** 订单不存在（或不是你的 —— 故意与「不存在」同码，防枚举他人订单） */
    public static final int ORDER_NOT_FOUND = 3001;
    /** 订单当前状态不允许该操作（状态机守卫，见 OrderService） */
    public static final int ORDER_STATE_INVALID = 3002;
    /**
     * V2：库存不足（票卖完了，或剩余票数不够这次买的数量）。
     *
     * 【为什么不复用 1002 NOT_FOUND】让用户看到明确原因（"票不够了"）比
     * 「资源不存在」体验好得多；而且前端可以据此刷新列表显示真实余票。
     */
    public static final int STOCK_NOT_ENOUGH = 3003;

    /**
     * V2：重复提交（并发幂等冲突）。
     *
     * 正常幂等（串行重复）走「预检命中直接返回」，不会走到这里；
     * 只有两个相同 requestId 同时到达、都通过预检后又撞了唯一索引，才会回滚并抛这个码。
     * 前端收到后用同一个 requestId 重试一次即可（重试会命中预检返回原订单）。
     */
    public static final int DUPLICATE_SUBMIT = 3004;

    /** 系统内部错误（未捕获异常兜底，细节只进日志，绝不透出堆栈） */
    public static final int SYSTEM_ERROR = 5000;
}
