package com.trainlog.service;

import com.trainlog.dto.LoginDTO;
import com.trainlog.dto.RegisterDTO;
import com.trainlog.vo.LoginVO;
import com.trainlog.vo.RegisterConfigVO;
import com.trainlog.vo.UserVO;

public interface AuthService {

    LoginVO login(LoginDTO dto, String ip);

    RegisterConfigVO registerConfig();

    LoginVO register(RegisterDTO dto, String ip);

    UserVO getCurrentUser(String username);
}
