package com.myh5.server.prediction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表评论的请求体。
 *
 * 校验放在 DTO 上用注解做，而不是在 Service 里写 if ——
 * 这样校验规则和字段定义长在一起，加一个字段就顺手加一条规则，不会漏。
 * 非法入参会被 @Valid 拦下并转成 1001（见 GlobalExceptionHandler）。
 *
 * ⚠️ 前端也会做一次同样的长度校验，但那是【体验】，这里才是【安全】：
 * 前端校验能被绕过（改 JS、直接打接口），后端的这道才是真正拦得住的那道。
 */
public record CreateCommentRequest(

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论最多 500 字")
        String content
) {
}
