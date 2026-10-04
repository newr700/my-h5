package com.myh5.server.config;

import com.myh5.server.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 装配：把 AuthInterceptor 挂到请求链上，并划定保护范围。
 *
 * ── 保护名单的设计原则：默认放行，显式保护 ──────────────────
 * 只拦截明确需要登录的路径（/order/**、/user/**），其余公开。
 * 反过来的方案（拦截一切 + 长长的排除名单）更「安全默认」，
 * 但每加一个公开端点都要记得改排除名单，忘了就是接口 401 玄学。
 * 实习项目量级选前者；金融级系统应选后者 —— 知道两种都存在的理由即可。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/order/**", "/user/**");
        // /standings、/matches、/auth/**、/doc.html、/v3/api-docs 等公开端点不在名单里，直接放行
    }
}
