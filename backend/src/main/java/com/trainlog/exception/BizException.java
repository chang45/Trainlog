package com.trainlog.exception;

/**
 * 业务异常，携带 HTTP 状态码，交给全局异常处理器统一转成 JSON。
 */
public class BizException extends RuntimeException {

    private final int httpStatus;

    public BizException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
