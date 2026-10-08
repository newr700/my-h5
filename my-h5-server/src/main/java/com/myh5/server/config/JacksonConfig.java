package com.myh5.server.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * LocalDateTime 的全局序列化格式 —— 补 application.yml 里 spring.jackson.date-format 的盲区。
 *
 * ── 踩坑现场（本项目真实发生）──────────────────────────────
 * yml 里配的 date-format: yyyy-MM-dd HH:mm:ss 只对老式 java.util.Date 生效，
 * 对 Java 8 时间类型（LocalDateTime）【不生效】—— 接口会输出 ISO 格式
 * "2026-10-11T19:30:00"，和工程手册 4.3 裁定的 "yyyy-MM-dd HH:mm:ss" 不一致。
 * 这是 Spring Boot 文档里不显眼、实际项目里必踩的坑：
 * 全局格式要覆盖新时间 API，必须显式注册 JavaTimeModule 的序列化器。
 *
 * （技能点：Jackson 自定义序列化；「配置没生效」类问题的排查思路——先怀疑作用域）
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(FORMATTER))
                .deserializers(new LocalDateTimeDeserializer(FORMATTER));
    }
}
