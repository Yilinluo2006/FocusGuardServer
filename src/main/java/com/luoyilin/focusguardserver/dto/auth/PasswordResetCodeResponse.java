package com.luoyilin.focusguardserver.dto.auth;

public class PasswordResetCodeResponse {

    private final String message;
    // 保存准备返回给客户端的统一提示

    public PasswordResetCodeResponse(String message) {
        this.message = message;
    }
    // 创建响应对象时写入提示内容

    public String getMessage() {
        return message;
    }
    // Jackson 通过 getter 把 message 转换成 JSON 字段
}
