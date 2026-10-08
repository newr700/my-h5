package com.myh5.server.common;

/**
 * 业务异常 —— Service 层发现业务规则不满足时抛出（如「订单状态不允许取消」），
 * 由 GlobalExceptionHandler 统一转成 Result，业务代码不用到处 try-catch。
 *
 * 约定：错误码必须已登记在《工程实施手册》4.2 错误码表，不许临时编一个。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
