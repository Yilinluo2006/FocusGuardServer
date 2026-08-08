package com.luoyilin.focusguardserver.dto.auth;

public class EmailVerificationResponse {

    private final String message;
    // 返回发送验证码或验证邮箱后的结果提示

    public EmailVerificationResponse(String message) {
        this.message = message;
    }
    // 创建只包含提示文字的响应对象

    public String getMessage() {
        return message;
    }
    // 返回响应中的提示文字
}
