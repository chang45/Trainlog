package com.trainlog.vo;

public record LoginVO(String token, long expiresIn, UserVO user) {
}
