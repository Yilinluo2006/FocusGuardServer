package com.luoyilin.focusguardserver.dto.auth;

import com.luoyilin.focusguardserver.model.User;

import java.time.LocalDateTime;

public class RegisterResponse {

    private final Long id;
    // 返回新用户的数据库主键

    private final String username;
    // 返回用户显示名称

    private final String email;
    // 返回注册邮箱

    private final boolean emailVerified;
    // 返回邮箱是否已经通过验证

    private final LocalDateTime createdAt;
    // 返回账号创建时间

    public RegisterResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
        this.createdAt = user.getCreatedAt();
    }
    // 从 User 实体中提取允许返回给客户端的数据

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}