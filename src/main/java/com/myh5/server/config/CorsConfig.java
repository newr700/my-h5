package com.myh5.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置 —— 开发期前端走 Vite 代理（浏览器视角是同源），用不到本配置；
 * 这是为生产环境「前后端不同域名」预备的。
 *
 * ⚠️ 上线前必须改：allowedOriginPatterns("*") 换成前端真实域名白名单，
 * 否则任何网站都能调你的接口（安全清单见工程手册 / 选型文档 4.5）。
 *
 * （技能点：CORS 是浏览器的行为约束，不是服务器拒绝服务——
 *  服务器照样处理请求，只是浏览器不把结果交给页面。所以防君子不防 curl，
 *  真正的访问控制要靠鉴权，不是靠 CORS）
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // TODO(上线前): 改为前端域名白名单
                .allowedMethods("GET", "POST")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
