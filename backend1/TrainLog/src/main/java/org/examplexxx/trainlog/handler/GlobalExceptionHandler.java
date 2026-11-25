package org.examplexxx.trainlog.handler;

import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：把 BizException 里的状态码和消息原样返回给前端。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        return ResponseEntity.status(e.getCode()).body(Result.<Void>error(e.getCode(), e.getMessage()));
    }

    /**
     * 请求体不是合法 JSON（比如少了括号、字段类型不对）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.<Void>error(400, "请求格式不正确"));
    }

    /**
     * 兜底：其余异常统一 500，避免把堆栈信息暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(500).body(Result.<Void>error(500, "服务器繁忙，请稍后再试"));
    }
}
