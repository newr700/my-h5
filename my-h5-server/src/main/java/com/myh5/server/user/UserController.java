package com.myh5.server.user;

import com.myh5.server.auth.AuthContext;
import com.myh5.server.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户接口（PRD-F8）。/user/** 在 AuthInterceptor 的保护名单里，
 * 所以走到这里时 token 一定有效，AuthContext 里一定有用户 id。
 */
@Tag(name = "用户")
@RestController
public class UserController {

    private final UserService userService;

    /**
     * V7 起 Controller 不再直接依赖 UserMapper：
     * 「查不到人怎么处理」「等级缺失怎么兜底」这类判断统一收进 UserService，
     * 否则 profile 与 expert-apply 两个方法会各写一遍、哪天只改一处就出现两种口径。
     * Controller 只管「接参 → 调 Service → 包 Result」，分层红线不越界。
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前登录用户资料")
    @GetMapping("/user/profile")
    public Result<UserProfileVo> profile() {
        // 「我是谁」不从前端传参拿，而是从 token 里取 ——
        // 前端传 userId 等于让用户自己报身份，改成别人的 id 就能看别人资料（IDOR 越权漏洞）
        return Result.ok(userService.profile(AuthContext.requireUserId()));
    }

    @Operation(summary = "上传头像（multipart/form-data，字段名 file）")
    @PostMapping("/user/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = AuthContext.requireUserId();
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.ok(avatarUrl);
    }

    @Operation(summary = "申请成为行业专家",
            description = "⚠️ 本项目无后台管理端，此接口是【模拟审核】：调用即通过，供体验评论功能。"
                    + "真实系统应由管理员审批，并把该接口收进管理员权限。")
    @PostMapping("/user/expert-apply")
    public Result<UserProfileVo> applyExpert() {
        return Result.ok(userService.applyExpert(AuthContext.requireUserId()));
    }
}
