package com.myh5.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myh5.server.common.ErrorCodes;
import com.myh5.server.common.Result;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权拦截器 —— 所有请求进入 Controller 之前的「门禁」。
 *
 * ── 这就是你想看的「JWT 令牌校验」的具体写法 ────────────────
 * 流程三步，对应一次请求的生命周期：
 *   ① preHandle（Controller 之前）：从 Authorization 头取 token → 验签 →
 *      把用户 id 放进 AuthContext；验不过就直接写回错误响应，请求根本到不了 Controller
 *   ② 业务代码执行：任何一层都能 AuthContext.requireUserId() 拿到「你是谁」
 *   ③ afterCompletion（响应之后）：清掉 ThreadLocal，防止线程复用串号
 *
 * ── 为什么用拦截器而不是每个 Controller 方法里手动校验 ────────
 * 同一份校验逻辑写进 N 个方法 = 漏写一个就是安全漏洞。
 * 「横切关注点」（每个请求都要做、但与业务无关的事）交给框架在统一入口做，
 * 这正是 AOP 思想，也是 axios 拦截器 / MP 拦截器 / 本拦截器反复出现的模式。
 *
 * ── 工程手册的裁定在这里体现 ────────────────────────────────
 * 业务失败 HTTP 状态仍是 200，成败看 body 里的 code（1101/1102），
 * 与前端 request.ts 的约定一致。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 约定格式：Authorization: Bearer <token>
        // "Bearer"（持票人）是 RFC 6750 的标准叫法：谁持有这张票，谁就是本人
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeFail(response, ErrorCodes.UNAUTHORIZED, "未登录或登录状态已失效");
            return false;   // 返回 false = 请求到此为止，不再进入 Controller
        }

        String token = header.substring(7);
        try {
            long userId = jwtUtil.parseUserId(token);
            AuthContext.setUserId(userId);
            return true;
        } catch (ExpiredJwtException e) {
            // 过期单独一个码：前端收到 1102 可以静默跳登录，体验上区别于「伪造/非法」
            writeFail(response, ErrorCodes.TOKEN_EXPIRED, "登录已过期，请重新登录");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            writeFail(response, ErrorCodes.UNAUTHORIZED, "登录状态无效，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 关键清理：线程要还回 Tomcat 线程池接下一单，ThreadLocal 不清理会串号
        // （详见 AuthContext 的注释 —— 这是 ThreadLocal 最经典的生产事故来源）
        AuthContext.clear();
    }

    /** 拦截器在 Controller 之外，异常不会被 @RestControllerAdvice 接住，所以自己写响应体 */
    private void writeFail(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);   // 工程手册裁定：业务失败不抛 HTTP 错误码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }
}
