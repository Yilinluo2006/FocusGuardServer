package com.luoyilin.focusguardserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;
    // 接收用户登录时提交的邮箱

    @NotBlank(message = "密码不能为空")
    @Size(
            min = 8,
            max = 64,
            message = "密码长度必须在 8 到 64 个字符之间"
    )
    private String password;
    // 接收用户登录时提交的明文密码

    public LoginRequest() {
    }
    // Jackson 将 JSON 转换为 LoginRequest 时需要无参构造方法

    public String getEmail() {
        return email;
    }
    // 返回登录邮箱

    public void setEmail(String email) {
        this.email = email;
    }
    // Jackson 使用这个方法把 JSON 中的 email 写入对象

    public String getPassword() {
        return password;
    }
    // 返回用户提交的密码

    public void setPassword(String password) {
        this.password = password;
    }
    // Jackson 使用这个方法把 JSON 中的 password 写入对象
}