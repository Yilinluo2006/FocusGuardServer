package com.luoyilin.focusguardserver.model;

public class AppLimit {

    private String packageName;
    // 保存应用包名

    private int limitMinutes;
    // 保存每天限制的分钟数

    private boolean enabled;
    // 保存该限制是否启用

    public AppLimit() {
    }
    // 无参构造方法：帮助 Spring 将收到的 JSON 转换成 AppLimit 对象

    public AppLimit(String packageName, int limitMinutes, boolean enabled) {
        this.packageName = packageName;
        this.limitMinutes = limitMinutes;
        this.enabled = enabled;
    }
    // 有参构造方法：方便我们直接使用三个属性创建对象

    public String getPackageName() {
        return packageName;
    }
    // 获取应用包名

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    // 修改应用包名

    public int getLimitMinutes() {
        return limitMinutes;
    }
    // 获取限制分钟数

    public void setLimitMinutes(int limitMinutes) {
        this.limitMinutes = limitMinutes;
    }
    // 修改限制分钟数

    public boolean isEnabled() {
        return enabled;
    }
    // 获取限制是否启用

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    // 修改限制是否启用
}