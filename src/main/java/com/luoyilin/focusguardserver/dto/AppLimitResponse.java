package com.luoyilin.focusguardserver.dto;

public class AppLimitResponse {

    private final Long id;
    // 返回数据库为这条记录生成的主键

    private final String packageName;
    // 返回被限制应用的包名

    private final int limitMinutes;
    // 返回每日限制分钟数

    private final boolean enabled;
    // 返回这条限制是否启用

    public AppLimitResponse(
            Long id,
            String packageName,
            int limitMinutes,
            boolean enabled
    ) {
        this.id = id;
        this.packageName = packageName;
        this.limitMinutes = limitMinutes;
        this.enabled = enabled;
    }
    // 通过构造方法一次性创建完整的响应对象

    public Long getId() {
        return id;
    }
    // 返回记录 ID

    public String getPackageName() {
        return packageName;
    }
    // 返回应用包名

    public int getLimitMinutes() {
        return limitMinutes;
    }
    // 返回限制分钟数

    public boolean isEnabled() {
        return enabled;
    }
    // 返回限制是否启用
}