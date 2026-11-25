package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.LoginDTO;
import org.examplexxx.trainlog.vo.LoginVO;

public interface AuthService {

    /**
     * 账号密码登录，成功返回 jwt 和用户信息。
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册，成功直接返回 jwt（注册即登录）。
     */
    LoginVO register(LoginDTO dto);
}
