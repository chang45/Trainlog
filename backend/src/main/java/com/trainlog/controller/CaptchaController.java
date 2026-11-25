package com.trainlog.controller;

import com.trainlog.result.Result;
import com.trainlog.service.CaptchaService;
import com.trainlog.service.LoginGuard;
import com.trainlog.utils.ClientIpUtil;
import com.trainlog.vo.CaptchaVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CaptchaController {

    private final CaptchaService captchaService;
    private final LoginGuard loginGuard;

    public CaptchaController(CaptchaService captchaService, LoginGuard loginGuard) {
        this.captchaService = captchaService;
        this.loginGuard = loginGuard;
    }

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha(HttpServletRequest request) {
        loginGuard.checkCaptchaRate(ClientIpUtil.getIp(request));
        return Result.success(captchaService.create());
    }
}
