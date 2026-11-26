package org.examplexxx.trainlog.http;


/**
 * HTTP 调用异常。
 * code = 0 表示网络层失败（连接超时、DNS 失败等），其它表示服务端返回的 HTTP 状态码。
 */

public class HttpException extends RuntimeException {


    private final int code;
    private final String responseBody;



    public HttpException(int code, String message) {
        super(message);
        this.code = code;
        this.responseBody = null;
    }

    public HttpException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.responseBody = null;
    }

    public HttpException(int code, String message, String responseBody) {
        super(message);
        this.code = code;
        this.responseBody = responseBody;
    }

    public int getCode() {
        return code;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
