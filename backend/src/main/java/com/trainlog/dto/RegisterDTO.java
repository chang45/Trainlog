package com.trainlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,31}$",
                message = "用户名需以字母开头，3-32 位字母、数字或下划线")
        String username,

        @NotBlank(message = "请输入密码")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "密码需 8-64 位，且同时包含字母和数字")
        String password,

        @Size(max = 50, message = "昵称最多 50 个字符")
        String nickname,

        @Size(max = 64, message = "邀请码过长")
        String inviteCode,

        @NotBlank(message = "验证码已失效，请刷新")
        String captchaId,

        @NotBlank(message = "请输入图片验证码")
        String captchaCode
) {
}
