package com.luoyilin.focusguardserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordResetConfirmRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(
            max = 100,
            message = "邮箱长度不能超过 100 个字符"
    )
    private String email;
    // 接收需要重置密码的账号邮箱

    @NotBlank(message = "验证码不能为空")
    @Pattern(
            regexp = "\\d{6}",
            message = "验证码必须是 6 位数字"
    )
    private String code;
    // 接收用户邮箱中收到的六位验证码

    @NotBlank(message = "新密码不能为空")
    @Size(
            min = 8,
            max = 64,
            message = "新密码长度必须在 8 到 64 个字符之间"
    )
    private String newPassword;
    // 接收准备替换旧密码的新明文密码

    public PasswordResetConfirmRequest() {
    }
    // Jackson 把请求 JSON 转换成对象时需要无参数构造方法

    public String getEmail() {
        return email;
    }
    // 返回客户端提交的邮箱

    public void setEmail(String email) {
        this.email = email;
    }
    // Jackson 使用这个方法写入邮箱

    public String getCode() {
        return code;
    }
    // 返回客户端提交的验证码

    public void setCode(String code) {
        this.code = code;
    }
    // Jackson 使用这个方法写入验证码

    public String getNewPassword() {
        return newPassword;
    }
    // 返回客户端提交的新密码

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    // Jackson 使用这个方法写入新密码
}
