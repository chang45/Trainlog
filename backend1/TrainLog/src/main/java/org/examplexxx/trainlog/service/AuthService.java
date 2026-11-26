package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.LoginDTO;
import org.examplexxx.trainlog.dto.ProfileDTO;
import org.examplexxx.trainlog.vo.LoginVO;
import org.examplexxx.trainlog.vo.UserVO;

public interface AuthService {

    /**
     * 账号密码登录，成功返回 jwt 和用户信息。
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册，成功直接返回 jwt（注册即登录）。
     */
    LoginVO register(LoginDTO dto);

    /**
     * 当前登录用户信息（含身高体重）。
     */
    UserVO getCurrentUser();

    /**
     * 保存身高体重。
     */
    UserVO updateProfile(ProfileDTO dto);
}
