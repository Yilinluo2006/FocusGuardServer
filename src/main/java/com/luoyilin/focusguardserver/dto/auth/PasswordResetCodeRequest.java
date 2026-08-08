package com.luoyilin.focusguardserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(
            max = 100,
            message = "邮箱长度不能超过 100 个字符"
    )
    private String email;
    // 接收需要找回密码的账号邮箱

    public PasswordResetCodeRequest() {
    }
    // Jackson 把请求 JSON 转换成对象时需要无参数构造方法

    public String getEmail() {
        return email;
    }
    // 返回客户端提交的邮箱

    public void setEmail(String email) {
        this.email = email;
    }
    // Jackson 使用这个方法把 JSON 中的邮箱写入对象
}
