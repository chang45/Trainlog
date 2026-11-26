package org.examplexxx.trainlog.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import okhttp3.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTTP 响应封装。
 * 非 2xx 不会自动抛异常，需要判空/判断状态码时用 {@link #isSuccessful()}。
 */

public final class HttpResponse {


    private static  ObjectMapper MAPPER = new ObjectMapper();

    private final int code;
    private final String message;
    private final String body;
    private final Headers headers;

    public HttpResponse(int code, String message, String body, Headers headers) {
        this.code = code;
        this.message = message;
        this.body = body;
        this.headers = headers;
    }


    public String header(String name) {
        return headers == null ? null : headers.get(name);
    }

    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    /**
     * 把响应体按 JSON 反序列化成对象；非 2xx 或解析失败抛 {@link HttpException}。
     */
    public <T> T as(Class<T> type) {
        if (!isSuccessful()) {
            throw new HttpException(code, "HTTP " + code + " " + message, body);
        }
        try {
            return MAPPER.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new HttpException(code, "响应解析失败：" + e.getOriginalMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "HttpResponse{code=" + code + ", body=" + body + "}";
    }
}
