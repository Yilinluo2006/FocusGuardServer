package com.luoyilin.focusguardserver.dto.auth;

import com.luoyilin.focusguardserver.model.User;

public class LoginResponse {

    private final String token;
    // 登录成功后由后端生成的 JWT

    private final String tokenType;
    // 令牌类型，Bearer 是 JWT 常用的携带方式

    private final Long userId;
    // 当前登录用户的数据库主键

    private final String username;
    // 当前登录用户的显示名称

    private final String email;
    // 当前登录用户的邮箱

    private final boolean emailVerified;
    // 表示当前用户的邮箱是否已经验证

    public LoginResponse(String token, User user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
    }
    // 根据 JWT 和 User 实体创建登录响应，但不会返回密码哈希

    public String getToken() {
        return token;
    }
    // 返回 JWT

    public String getTokenType() {
        return tokenType;
    }
    // 返回令牌类型 Bearer

    public Long getUserId() {
        return userId;
    }
    // 返回用户 ID

    public String getUsername() {
        return username;
    }
    // 返回用户名

    public String getEmail() {
        return email;
    }
    // 返回邮箱

    public boolean isEmailVerified() {
        return emailVerified;
    }
    // 返回邮箱验证状态
}