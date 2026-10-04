package com.myh5.server.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper —— 继承 BaseMapper 就白得一整套 CRUD：
 * insert / selectById / selectOne(条件) / updateById / deleteById……
 * 这就是 MyBatis-Plus 的核心卖点：单表操作零 SQL。
 *
 * 代价要心里有数：生成的 SQL 你没写过，开了 SQL 日志（dev 配置里已开）
 * 才看得见它到底执行了什么 —— ORM 的便利和对 SQL 的掌控感是此消彼长的。
 *
 * （技能点：Mapper 接口没有实现类，MP 在启动时用 JDK 动态代理生成实现 ——
 *  「接口没有 implements 为什么能注入调用」是经典面试题）
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
