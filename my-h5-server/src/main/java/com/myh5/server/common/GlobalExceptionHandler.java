package com.myh5.server.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常兜底 —— 所有 Controller 抛出的异常在这里统一转成 {code, message, data}。
 *
 * 为什么业务失败仍返回 HTTP 200：成败由 code 表达，HTTP 状态码只留给框架级错误
 * （前端 request.ts 拦截器就是这么假设的，两边必须一致 —— 工程手册 4.2）。
 *
 * （技能点：@RestControllerAdvice 的原理是 AOP —— 在 Controller 外包一层，
 *  异常抛出来时由它接住，业务代码完全感知不到它的存在）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：错误码由抛出方指定 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @Valid 参数校验失败：透出第一个字段的错误原因，错误码固定 1001 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "参数校验失败"
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return Result.fail(ErrorCodes.PARAM_INVALID, message);
    }

    /**
     * 请求体根本不是合法 JSON（或类型对不上，比如 matchId 传了字符串）。
     * 不拦这个，客户端收到的是 5000「系统内部错误」—— 明明是调用方的锅，
     * 却报警成我们的系统错误，监控会被污染。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ErrorCodes.PARAM_INVALID, "请求体格式不正确");
    }

    /**
     * 未预料的异常：对外只说「系统内部错误」，细节只进日志。
     * 把堆栈直接返回给调用方 = 向攻击者泄露代码结构，绝不能做。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return Result.fail(5000, "系统内部错误");
    }
}
