package com.luoyilin.focusguardserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmailVerificationConfirmRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(
            max = 100,
            message = "邮箱长度不能超过 100 个字符"
    )
    private String email;
    // 接收需要完成验证的注册邮箱

    @NotBlank(message = "验证码不能为空")
    @Pattern(
            regexp = "\\d{6}",
            message = "验证码必须是 6 位数字"
    )
    private String code;
    // 接收用户从邮件中取得的六位数字验证码

    public EmailVerificationConfirmRequest() {
    }
    // Jackson 把 JSON 转换成对象时需要无参构造方法

    public String getEmail() {
        return email;
    }
    // 返回注册邮箱

    public void setEmail(String email) {
        this.email = email;
    }
    // Jackson 使用这个方法写入邮箱

    public String getCode() {
        return code;
    }
    // 返回六位验证码

    public void setCode(String code) {
        this.code = code;
    }
    // Jackson 使用这个方法写入验证码
}
