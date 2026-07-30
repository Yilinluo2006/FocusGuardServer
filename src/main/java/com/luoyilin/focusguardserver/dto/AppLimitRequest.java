package com.luoyilin.focusguardserver.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AppLimitRequest {

    @NotBlank(message = "应用包名不能为空")
    private String packageName;
    // 接收客户端提交的应用包名，并保证它不是空字符串

    @Min(value = 1, message = "限制时间至少为 1 分钟")
    @Max(value = 1440, message = "限制时间不能超过 1440 分钟")
    private int limitMinutes;
    // 接收每日限制时间，允许范围是 1 到 1440 分钟

    private boolean enabled;
    // 接收该应用限制是否启用

    public AppLimitRequest() {
        // 无参构造方法，Jackson 需要通过它创建 DTO 对象
    }

    public String getPackageName() {
        return packageName;
        // 返回应用包名
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        // 把客户端 JSON 中的 packageName 保存到当前对象
    }

    public int getLimitMinutes() {
        return limitMinutes;
        // 返回每日限制分钟数
    }

    public void setLimitMinutes(int limitMinutes) {
        this.limitMinutes = limitMinutes;
        // 把客户端 JSON 中的 limitMinutes 保存到当前对象
    }

    public boolean isEnabled() {
        return enabled;
        // 返回当前限制是否启用
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        // 把客户端 JSON 中的 enabled 保存到当前对象
    }
}