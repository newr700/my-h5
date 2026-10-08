package com.myh5.server.auth;

import com.myh5.server.auth.dto.LoginRequest;
import com.myh5.server.auth.dto.RegisterRequest;
import com.myh5.server.auth.vo.LoginVo;
import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（PRD-F6/F7）。公开端点，不在 AuthInterceptor 的保护名单里。
 *
 * @Valid 触发 DTO 上的校验注解：不合法的请求在这一步就被拦下（错误码 1001），
 * 根本进不了 Service —— 「后端必须再校验一次」的第一道闸，
 * 前端校验只是体验优化，curl 一条命令就能绕过它（PRD 5.2 的裁定）。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册", description = "成功后直接返回 token（注册即登录）")
    @PostMapping("/register")
    public Result<LoginVo> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    @Operation(summary = "登录", description = "失败统一报「用户名或密码错误」，不区分具体原因")
    @PostMapping("/login")
    public Result<LoginVo> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }
}
