package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.dto.LoginDTO;
import org.examplexxx.trainlog.dto.ProfileDTO;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.AuthService;
import org.examplexxx.trainlog.vo.LoginVO;
import org.examplexxx.trainlog.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * 登录：POST /auth/login
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /**
     * 注册：POST /auth/register，注册成功直接返回 token，前端存起来就是已登录状态
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody LoginDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    /**
     * 当前登录用户信息（含身高体重）
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser());
    }

    /**
     * 保存身高体重
     */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody ProfileDTO dto) {
        return Result.success("资料已保存", authService.updateProfile(dto));
    }
}
