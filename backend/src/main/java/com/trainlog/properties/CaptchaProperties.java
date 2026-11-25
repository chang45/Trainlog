package com.trainlog.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.captcha")
public class CaptchaProperties {

    private int length = 4;
    private int width = 120;
    private int height = 40;
    private long ttlSeconds = 120;
    private boolean logCode = false;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public boolean isLogCode() {
        return logCode;
    }

    public void setLogCode(boolean logCode) {
        this.logCode = logCode;
    }
}
