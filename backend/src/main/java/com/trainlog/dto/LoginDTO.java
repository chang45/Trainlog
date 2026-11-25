package com.trainlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "请输入用户名")
        @Size(max = 50, message = "用户名最多 50 个字符")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(max = 64, message = "密码最多 64 个字符")
        String password,

        @NotBlank(message = "验证码已失效，请刷新")
        String captchaId,

        @NotBlank(message = "请输入图片验证码")
        String captchaCode
) {
}
