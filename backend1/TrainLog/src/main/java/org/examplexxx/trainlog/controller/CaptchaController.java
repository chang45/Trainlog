package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import org.examplexxx.trainlog.vo.CaptchaVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha(HttpServletRequest request) {
        return Result.success(captchaService.create());
    }
}
