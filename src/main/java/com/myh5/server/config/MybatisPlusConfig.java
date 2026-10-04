package com.myh5.server.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * MP 的功能靠「拦截器链」挂载（和前端 axios 拦截器、后端 HandlerInterceptor
 * 是同一个思想的第三次出现 —— 在统一的位置对「流经的每一单」做加工）。
 * 分页插件的工作原理：拦截你写的 SELECT，自动在外面包一层 LIMIT，
 * 并额外发一条 COUNT 查询算总数 —— 所以开了 SQL 日志会看到两条 SQL。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // DbType.MYSQL：分页方言（LIMIT ?, ?）。
        // 测试用的 H2 开在 MySQL 兼容模式，认识这套方言，所以同一份配置两边都能跑
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
