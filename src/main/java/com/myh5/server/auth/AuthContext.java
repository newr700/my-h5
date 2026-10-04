package com.myh5.server.auth;

/**
 * 当前请求的用户上下文 —— 用 ThreadLocal 存放「这个请求是谁发的」。
 *
 * ── 为什么需要它 ────────────────────────────────────────────
 * 拦截器验完 token 知道了用户 id，Controller/Service 层要用。
 * 一路透传参数（每个方法都加个 userId 形参）当然也行，但污染每一层签名。
 * ThreadLocal 相当于「当前线程的随身口袋」：Tomcat 一个请求由一个线程处理，
 * 所以在请求生命周期的任何地方都能拿到，又天然与其他请求隔离。
 *
 * ── ⚠️ 用完必须 remove()，这是 ThreadLocal 最大的坑 ─────────
 * Tomcat 的线程是【池化复用】的：请求 A 处理完，线程不还回销毁，
 * 而是接下一单请求 B。如果 A 忘了 remove，B 就会读到 A 的用户 id ——
 * 「用户看到了别人的数据」这类灵异 bug 的经典来源。
 * 清理动作统一放在 AuthInterceptor.afterCompletion 里，业务代码不用管。
 *
 * （技能点：ThreadLocal 原理与内存泄漏；线程池场景的经典面试题）
 */
public final class AuthContext {

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    /** 没登录的公开接口里调用会返回 null —— 调用方自己保证只在受保护接口里用 */
    public static Long getUserId() {
        return CURRENT_USER.get();
    }

    /** 同 getUserId，但拿不到时直接抛 1101 —— 受保护接口里的推荐用法 */
    public static Long requireUserId() {
        Long userId = CURRENT_USER.get();
        if (userId == null) {
            // 理论上拦截器已挡住，这里兜底：防御性编程 —— 每一层都假设上一层可能失守
            throw new IllegalStateException("当前请求没有登录上下文");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
