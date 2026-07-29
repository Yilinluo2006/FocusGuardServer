package com.luoyilin.focusguardserver.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        // 保存字段名称和对应的错误信息

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );
        // 取出全部字段错误，并保存到 fieldErrors

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 400);
        response.put("message", "请求参数校验失败");
        response.put("errors", fieldErrors);
        // 组织最终返回给客户端的 JSON 数据

        return ResponseEntity.badRequest().body(response);
        // 返回 HTTP 400 和错误信息
    }
}