package com.trainlog.vo;

public record CaptchaVO(String captchaId, String image, int expiresIn) {
}
