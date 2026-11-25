package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaVO {
    private String id;
    private String image;
    private int ttlSeconds;
}
