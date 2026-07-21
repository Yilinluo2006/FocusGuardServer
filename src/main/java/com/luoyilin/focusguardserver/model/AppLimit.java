package com.luoyilin.focusguardserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_limits")
public class AppLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 数据库主键，由 MySQL 自动递增生成

    @Column(name = "package_name", nullable = false, unique = true)
    private String packageName;
    // 保存应用包名，例如 com.instagram.android

    @Column(name = "limit_minutes", nullable = false)
    private int limitMinutes;
    // 保存每天允许使用的分钟数

    @Column(nullable = false)
    private boolean enabled;
    // 保存该限制当前是否启用

    public AppLimit() {
    }
    // JPA 和 JSON 转换需要无参数构造方法

    public AppLimit(String packageName, int limitMinutes, boolean enabled) {
        this.packageName = packageName;
        this.limitMinutes = limitMinutes;
        this.enabled = enabled;
    }
    // 创建 AppLimit 对象时初始化三个业务属性

    public Long getId() {
        return id;
    }
    // 返回数据库记录的主键

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    // 读取或修改应用包名

    public int getLimitMinutes() {
        return limitMinutes;
    }

    public void setLimitMinutes(int limitMinutes) {
        this.limitMinutes = limitMinutes;
    }
    // 读取或修改限制分钟数

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    // 读取或修改限制启用状态
}