package org.examplexxx.trainlog.service;
import org.examplexxx.trainlog.vo.CaptchaVO;

public interface CaptchaService {

    CaptchaVO create();

    boolean verify(String captchaId, String code);
}
