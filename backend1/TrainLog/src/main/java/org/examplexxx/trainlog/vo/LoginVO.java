package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回。
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** jwt 令牌 */
    private String token;

    /** 前端要把 token 放在这个请求头里，例如 admin-token */
    private String tokenName;

    /** 有效期（秒） */
    private long expiresIn;

    /** 当前登录用户信息 */
    private UserVO user;
}
