package org.examplexxx.trainlog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "app.captcha")
public class CaptchaProperties {

    private int length = 4;
    private int width = 120;
    private int height = 40;
    private long ttlSeconds = 120;
    private boolean logCode = false;
}
