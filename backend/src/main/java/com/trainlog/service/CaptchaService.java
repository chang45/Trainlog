package com.trainlog.service;

import com.trainlog.vo.CaptchaVO;

public interface CaptchaService {

    CaptchaVO create();

    boolean verify(String captchaId, String code);
}
