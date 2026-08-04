package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.auth.RegisterRequest;
import com.luoyilin.focusguardserver.dto.auth.RegisterResponse;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    // 保存 Spring 提供的注册业务对象

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    // 构造器注入：Spring 自动传入 AuthService

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request);
        // 调用 Service 检查数据、处理密码并保存用户

        RegisterResponse response =
                new RegisterResponse(user);
        // 把 User 实体转换成不包含密码哈希的响应对象

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        // 返回 HTTP 201 和注册成功后的用户信息
    }
}