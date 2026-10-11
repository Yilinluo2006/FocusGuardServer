package com.luoyilin.focusguardserver.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIdentityConflict(org.springframework.dao.DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(Map.of("status", 409, "message", "账号信息发生冲突，请刷新后重试"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        // 保存字段名称和对应的参数校验错误信息

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
        // 组织参数校验失败时返回的 JSON 数据

        return ResponseEntity.badRequest().body(response);
        // 返回 HTTP 400 和错误信息
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        // 创建用于保存错误响应内容的 Map

        response.put(
                "status",
                exception.getStatusCode().value()
        );
        // 取得异常中的 HTTP 状态码，例如 404 或 409

        response.put(
                "message",
                exception.getReason()
        );
        // 取得创建异常时填写的错误原因

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(response);
        // 使用异常中的状态码和消息生成 JSON 响应
    }
}
