package com.myh5.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置 —— 开发期前端走 Vite 代理（浏览器视角是同源），用不到本配置；
 * 这是为生产环境「前后端不同域名」预备的。
 *
 * 上线前必须改：allowedOriginPatterns("*") 换成前端真实域名白名单，
 * 否则任何网站都能调你的接口（安全清单见工程手册 / 选型文档 4.5）：
 * 《现在写 * 意味着任何网站都能调你的接口——本地演示无所谓，上线是安全隐患。》
 *
 * （技能点：CORS 是浏览器的行为约束，不是服务器拒绝服务——
 *  服务器照样处理请求，只是浏览器不把结果交给页面。所以防君子不防 curl，
 *  真正的访问控制要靠鉴权，不是靠 CORS）
 *  《CORS 只是浏览器不把结果交给页面，curl / Postman 照样能调。
 *  所以真正的访问控制靠的是你那个 AuthInterceptor（JWT 鉴权），而不是 CORS。》
 *
 *  @Configuration + 实现 WebMvcConfigurer：把这段配置注册进 Spring MVC。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")// 对所有接口路径生效
                .allowedOriginPatterns("*") // 允许任意来源 TODO(上线前): 改为前端域名白名单
                .allowedMethods("GET", "POST")// 只放行读和写，不接受 DELETE/PUT 等
                .allowedHeaders("*")//  请求头任意（比如带 Authorization 的 token 头）
                .maxAge(3600);// 预检请求缓存 1 小时，减少 OPTIONS 探测
    }
}
