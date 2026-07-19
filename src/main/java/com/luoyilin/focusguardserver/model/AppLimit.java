package com.luoyilin.focusguardserver.model;;

public class AppLimit {

    private String packageName;
    // 保存应用包名，例如 Instagram 的包名

    private int limitMinutes;
    // 保存每天允许使用的分钟数

    private boolean enabled;
    // 表示当前限制是否启用

    public AppLimit(String packageName, int limitMinutes, boolean enabled) {
        this.packageName = packageName;
        this.limitMinutes = limitMinutes;
        this.enabled = enabled;
    }
    // 构造方法：创建 AppLimit 对象并初始化三个属性

    public String getPackageName() {
        return packageName;
    }
    // 返回应用包名

    public int getLimitMinutes() {
        return limitMinutes;
    }
    // 返回限制时长

    public boolean isEnabled() {
        return enabled;
    }
    // 返回限制是否启用
}