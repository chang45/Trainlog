package com.trainlog.controller;

import com.trainlog.context.BaseContext;
import com.trainlog.dto.LoginDTO;
import com.trainlog.dto.RegisterDTO;
import com.trainlog.result.Result;
import com.trainlog.service.AuthService;
import com.trainlog.utils.ClientIpUtil;
import com.trainlog.vo.LoginVO;
import com.trainlog.vo.RegisterConfigVO;
import com.trainlog.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success("登录成功", authService.login(dto, ClientIpUtil.getIp(request)));
    }

    @GetMapping("/register-config")
    public Result<RegisterConfigVO> registerConfig() {
        return Result.success(authService.registerConfig());
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        return Result.success("注册成功", authService.register(dto, ClientIpUtil.getIp(request)));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser(BaseContext.getUsername()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已退出登录", null);
    }
}
