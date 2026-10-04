package com.myh5.server.user;

import com.myh5.server.auth.AuthContext;
import com.myh5.server.common.BizException;
import com.myh5.server.common.ErrorCodes;
import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口（PRD-F8）。/user/** 在 AuthInterceptor 的保护名单里，
 * 所以走到这里时 token 一定有效，AuthContext 里一定有用户 id。
 */
@Tag(name = "用户")
@RestController
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(summary = "获取当前登录用户资料")
    @GetMapping("/user/profile")
    public Result<UserProfileVo> profile() {
        // 「我是谁」不从前端传参拿，而是从 token 里取 ——
        // 前端传 userId 等于让用户自己报身份，改成别人的 id 就能看别人资料
        // （这就是越权漏洞 IDOR，OWASP Top 10 常客）
        Long userId = AuthContext.requireUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            // token 合法但人没了（账号被删）—— 极端但可能，按未登录处理
            throw new BizException(ErrorCodes.UNAUTHORIZED, "账号不存在，请重新登录");
        }
        return Result.ok(new UserProfileVo(user.getId(), user.getUsername(),
                user.getNickname(), user.getCreatedAt()));
    }
}
