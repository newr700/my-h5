package com.myh5.server.auth;

import cn.hutool.core.util.StrUtil;
// 注意包路径：BCrypt 在 crypto.digest 子包下，不在 crypto 根包 ——
// Hutool 的类按「功能域」分包，import 错了就是找不到符号（这次编译失败的实际原因）
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myh5.server.auth.dto.LoginRequest;
import com.myh5.server.auth.dto.RegisterRequest;
import com.myh5.server.auth.vo.LoginVo;
import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import com.myh5.server.user.UserEntity;
import com.myh5.server.user.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 认证服务 —— 注册与登录的业务规则都在这里。
 *
 * ── 分层红线（工程手册第 3 章）────────────────────────────
 * Controller 只接参数、调 Service、包 Result；所有「规则」都长在 Service。
 * 判断标准：这段逻辑换个入口（比如将来加个定时任务也要创建用户）还能不能复用？
 * 能 → 属于 Service。
 *
 * ── 密码安全三原则（面试必考，逐条对照代码看）──────────────
 * ① 绝不存明文：存的是 BCrypt(password) 的哈希，库被偷了也还原不出密码
 * ② 哈希必须带盐且慢：BCrypt 每次哈希自动加随机盐（同一个密码两次哈希结果不同），
 *    且计算刻意慢（可调 cost 因子），让暴力破解的成本高到不现实
 *    —— MD5/SHA256 太快，彩虹表秒破，绝对不能用于密码
 * ③ 登录失败模糊报错：「用户名或密码错误」，绝不告诉攻击者到底是哪个错
 */
@Service
public class AuthService {

    /**
     * 假哈希，用途见 login()：类加载时算一次（约几百毫秒），之后复用。
     * 不能写成固定字符串 —— 手搓的 BCrypt 哈希格式稍有不合法，checkpw 会抛异常而不是返回 false。
     */
    private static final String DUMMY_HASH = BCrypt.hashpw("dummy-password-for-timing");

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 注册：校验唯一性 → 哈希密码 → 落库 → 直接签发 token（注册即登录） */
    public LoginVo register(RegisterRequest req) {
        // LambdaQueryWrapper：类型安全的条件构造器。
        // UserEntity::getUsername 是方法引用，写错字段名编译期就报错 ——
        // 比手写 "username = 'xxx'" 字符串安全（字符串写错运行时才炸）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, req.username()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCodes.USERNAME_EXISTS, "用户名已被注册");
        }

        UserEntity user = new UserEntity();
        user.setUsername(req.username());
        // BCrypt.hashpw 每次生成随机盐，所以同一密码的哈希值每次都不同；
        // 校验时用 checkpw 而不是「再哈希一次对比」—— 这正是盐的意义
        user.setPasswordHash(BCrypt.hashpw(req.password()));
        user.setNickname(StrUtil.isBlank(req.nickname()) ? req.username() : req.nickname());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发兜底：两个请求同时通过上面的 count 检查，只有一个能插入成功，
            // 输的那个撞上 uk_app_user_username 唯一约束 —— 数据库约束是最后一道防线
            throw new BizException(ErrorCodes.USERNAME_EXISTS, "用户名已被注册");
        }

        // insert 后 MP 会把自增主键回填进实体（useGeneratedKeys），user.getId() 此时有值
        return new LoginVo(jwtUtil.generate(user.getId()), user.getId(), user.getUsername(), user.getNickname());
    }

    /** 登录：查用户 → 验密码 → 签 token。两种失败报同一句错（防枚举） */
    public LoginVo login(LoginRequest req) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, req.username()));

        // 注意写法：用户不存在时也照常执行一次 checkpw（对假哈希校验），
        // 而不是短路 return —— 否则「不存在就秒回、存在就多算几百毫秒哈希」，
        // 攻击者掐表就能分辨用户名存不存在（计时攻击）。小细节，大安全观。
        String hashToCheck = user != null ? user.getPasswordHash() : DUMMY_HASH;
        boolean passwordOk = BCrypt.checkpw(req.password(), hashToCheck) && user != null;

        if (!passwordOk) {
            throw new BizException(ErrorCodes.BAD_CREDENTIALS, "用户名或密码错误");
        }
        return new LoginVo(jwtUtil.generate(user.getId()), user.getId(), user.getUsername(), user.getNickname());
    }
}
