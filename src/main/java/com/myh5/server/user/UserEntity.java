package com.myh5.server.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（Entity = 数据库表的 Java 镜像，一张表一个类）。
 *
 * @TableName 显式声明表名：类名 AppUser → MP 默认猜 app_user 正好对，
 * 但显式写出来，将来改类名不会悄悄改映射 —— 显式优于约定，关键处留证据。
 *
 * @Data 是 Lombok 注解：编译期自动生成 getter/setter/toString/equals。
 */
@Data
@TableName("app_user")
public class UserEntity {

    @TableId(type = IdType.AUTO)   // 对应建表语句的 AUTO_INCREMENT
    private Long id;

    private String username;

    /** BCrypt 哈希值。它存在于 Entity 里，但永远不会出现在任何 VO 里 —— 见 LoginVo 注释 */
    private String passwordHash;

    private String nickname;

    /**
     * 头像相对路径（如 /uploads/12_1690000000000.png）。
     * 列名是 snake_case avatar_url：MP 下划线转驼峰已开启，但显式 @TableField
     * 写明列名最稳（哪天关了驼峰映射也不会变 null）。
     * null/空 = 未设置头像，前端显示昵称首字母占位。
     */
    @TableField("avatar_url")
    private String avatarUrl;

    private LocalDateTime createdAt;
}
