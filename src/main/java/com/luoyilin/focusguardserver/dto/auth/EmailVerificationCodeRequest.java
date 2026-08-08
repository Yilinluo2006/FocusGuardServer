package com.luoyilin.focusguardserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailVerificationCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(
            max = 100,
            message = "邮箱长度不能超过 100 个字符"
    )
    private String email;
    // 接收需要重新发送注册验证码的邮箱

    public EmailVerificationCodeRequest() {
    }
    // Jackson 把 JSON 转换成对象时需要无参构造方法

    public String getEmail() {
        return email;
    }
    // 返回请求中的邮箱

    public void setEmail(String email) {
        this.email = email;
    }
    // Jackson 使用这个方法写入邮箱
}
