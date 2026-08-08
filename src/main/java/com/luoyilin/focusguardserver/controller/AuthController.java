package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.auth.LoginRequest;
import com.luoyilin.focusguardserver.dto.auth.LoginResponse;
import com.luoyilin.focusguardserver.dto.auth.PasswordResetCodeRequest;
import com.luoyilin.focusguardserver.dto.auth.PasswordResetCodeResponse;
import com.luoyilin.focusguardserver.dto.auth.PasswordResetConfirmRequest;
import com.luoyilin.focusguardserver.dto.auth.PasswordResetConfirmResponse;
import com.luoyilin.focusguardserver.dto.auth.RegisterRequest;
import com.luoyilin.focusguardserver.dto.auth.RegisterResponse;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.AuthService;
import com.luoyilin.focusguardserver.service.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }
    // Spring 自动把登录注册服务和密码重置服务注入 Controller

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

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetCodeResponse> requestPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeRequest request
    ) {
        passwordResetService.requestResetCode(request.getEmail());
        // 邮箱存在时生成并发送验证码；不存在时不透露账号是否存在

        PasswordResetCodeResponse response =
                new PasswordResetCodeResponse(
                        "如果邮箱已注册，验证码将发送到该邮箱"
                );

        return ResponseEntity.ok(response);
    }
    // 接收验证码请求，并始终返回相同的 HTTP 200 提示

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<PasswordResetConfirmResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        passwordResetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
        );
        // 校验验证码并把账号密码更新为新密码

        PasswordResetConfirmResponse response =
                new PasswordResetConfirmResponse(
                        "密码重置成功，请使用新密码登录"
                );

        return ResponseEntity.ok(response);
    }
    // 接收重置确认请求，成功时返回 HTTP 200
}
