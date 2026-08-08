package com.luoyilin.focusguardserver.dto.auth;

public class PasswordResetConfirmResponse {

    private final String message;
    // 保存密码重置成功后返回给客户端的提示

    public PasswordResetConfirmResponse(String message) {
        this.message = message;
    }
    // 创建响应对象时写入提示内容

    public String getMessage() {
        return message;
    }
    // Jackson 通过 getter 把 message 转换成 JSON 字段
}
