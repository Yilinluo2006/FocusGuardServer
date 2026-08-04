package com.luoyilin.focusguardserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(
            min = 3,
            max = 50,
            message = "用户名长度必须在 3 到 50 个字符之间"
    )
    private String username;
    // 接收用户在应用中显示的用户名

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(
            max = 100,
            message = "邮箱长度不能超过 100 个字符"
    )
    private String email;
    // 接收登录和找回密码所使用的邮箱

    @NotBlank(message = "密码不能为空")
    @Size(
            min = 8,
            max = 64,
            message = "密码长度必须在 8 到 64 个字符之间"
    )
    private String password;
    // 接收明文密码，后续由 AuthService 转换成密码哈希

    public RegisterRequest() {
    }
    // Jackson 把 JSON 转换成对象时需要无参构造方法

    public String getUsername() {
        return username;
    }
    // 返回用户名

    public void setUsername(String username) {
        this.username = username;
    }
    // Jackson 使用这个方法写入用户名

    public String getEmail() {
        return email;
    }
    // 返回邮箱

    public void setEmail(String email) {
        this.email = email;
    }
    // Jackson 使用这个方法写入邮箱

    public String getPassword() {
        return password;
    }
    // 返回用户提交的明文密码

    public void setPassword(String password) {
        this.password = password;
    }
    // Jackson 使用这个方法写入密码
}