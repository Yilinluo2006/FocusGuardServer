package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.auth.LoginRequest;
import com.luoyilin.focusguardserver.dto.auth.LoginResponse;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    // Spring 自动把 AuthService 注入 Controller

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request);
        RegisterResponse response = new RegisterResponse(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    // 接收 POST /api/auth/register，成功时返回 HTTP 201

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    // 接收 POST /api/auth/login，验证成功时返回 HTTP 200 和 JWT
}