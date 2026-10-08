package com.myh5.server.config;

import com.myh5.server.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 装配：把 AuthInterceptor 挂到请求链上，并划定保护范围 + 暴露头像静态资源。
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
    private final String uploadDir;

    /**
     * uploadDir 从 application.yml 的 app.upload-dir 读（@Value 注入）。
     * 多一个构造参数就让 Spring 把配置值填进来 —— 比自己读配置文件干净。
     */
    public WebMvcConfig(AuthInterceptor authInterceptor,
                        @Value("${app.upload-dir}") String uploadDir) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/order/**", "/user/**");
        // /standings、/matches、/auth/**、/doc.html、/v3/api-docs、/uploads/** 等公开端点不在名单里，直接放行
    }

    /**
     * 关键一步：把磁盘上的上传目录映射成浏览器可访问的 URL 前缀 /uploads/**。
     *
     * 文件存到磁盘后，浏览器默认是访问不到的 —— 静态资源必须显式暴露，否则
     * 头像 <img src="/uploads/xxx.png"> 永远 404。这行配置让
     * http://localhost:8080/uploads/xxx.png 能取到 ./uploads/xxx.png。
     * （技能点：Spring 静态资源映射 —— file: 前缀指本地文件系统）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file: 前缀 = 本地文件系统路径；末尾补 / 保证路径拼接正确
        String location = "file:" + uploadDir + (uploadDir.endsWith("/") ? "" : "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600); // 1 小时缓存，减少重复下载
    }
}
