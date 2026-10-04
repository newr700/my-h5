package com.myh5.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口 —— 整个后端进程从这里启动。
 *
 * @SpringBootApplication 一个注解顶三个：
 * 配置类（@Configuration）+ 开启自动配置（@EnableAutoConfiguration）
 * + 组件扫描（@ComponentScan，扫本包及所有子包）。
 * 所以新建的 Controller/Service 必须放在 com.myh5.server 包或其子包下，否则扫不到。
 *
 * @MapperScan：Mapper 接口分散在各业务包下（standings/order/user/match），
 * 显式声明扫描根包，MP 才能为它们生成代理实现。
 * （技能点：自动配置与组件扫描范围；Mapper 的代理生成机制见 UserMapper 注释）
 */
@SpringBootApplication
@MapperScan("com.myh5.server")
public class H5ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(H5ServerApplication.class, args);
    }
}
