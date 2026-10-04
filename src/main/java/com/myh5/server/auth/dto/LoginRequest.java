package com.myh5.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 登录请求。规则说明见 RegisterRequest 的类注释（DTO 的意义与 Mass Assignment）。 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
