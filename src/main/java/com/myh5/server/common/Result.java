package com.myh5.server.common;

/**
 * 统一响应外壳 —— 与前端 src/api/request.ts 拦截器签死的契约：{ code, message, data }。
 * code = 0 表示业务成功；失败时 data 为 null，message 是给用户看的一句话（前端直接 toast）。
 *
 * 为什么用 record：Java 16+ 的 record 一行声明不可变值对象，
 * 编译器自动生成构造器、访问器（code()/message()/data()）、equals、toString，不需要 Lombok。
 * 响应对象天然不可变，正好匹配 record 的语义。
 * （技能点：record；不可变对象；Jackson 原生支持 record 序列化）
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
