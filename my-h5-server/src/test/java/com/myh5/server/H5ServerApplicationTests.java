package com.myh5.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 冒烟测试：能启动、Spring 上下文能装配成功，就算通过。
 * 真正的单测（MockMvc 测接口、H2 测 SQL）按四周计划 W3 补齐。
 */
@SpringBootTest
class H5ServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
