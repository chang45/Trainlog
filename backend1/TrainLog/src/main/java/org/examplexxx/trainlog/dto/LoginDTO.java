package org.examplexxx.trainlog.dto;

import lombok.Data;

/**
 * 登录入参。
 * 项目没有引入 spring-boot-starter-validation，所以这里不加校验注解，
 * 参数合法性由 AuthServiceImpl 手动判断。
 */
@Data
public class LoginDTO {

    /** 用户名 */
    private String username;

    /** 密码（明文，交给服务端 Argon2 校验） */
    private String password;

    /** 昵称（仅注册使用，选填，不填则用用户名） */
    private String nickname;

    /** 验证码唯一标识，前端从 /api/captcha 返回的 id 里拿 */
    private String captchaId;

    /** 用户输入的图片验证码 */
    private String captchaCode;
}
