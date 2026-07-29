package com.luoyilin.focusguardserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "app_limits")
public class AppLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 数据库主键，由 MySQL 自动递增生成

    @NotBlank(message = "应用包名不能为空")
    @Column(name = "package_name", nullable = false, unique = true)
    private String packageName;
    // 包名不能是 null、空字符串或只有空格

    @Min(value = 1, message = "限制时间至少为 1 分钟")
    @Max(value = 1440, message = "限制时间不能超过 1440 分钟")
    @Column(name = "limit_minutes", nullable = false)
    private int limitMinutes;
    // 限制时间必须处于 1 到 1440 分钟之间

    @Column(nullable = false)
    private boolean enabled;
    // 保存限制是否启用

    public AppLimit() {
    }
    // JPA 和 JSON 转换需要无参数构造方法

    public AppLimit(String packageName, int limitMinutes, boolean enabled) {
        this.packageName = packageName;
        this.limitMinutes = limitMinutes;
        this.enabled = enabled;
    }
    // 创建对象时初始化三个业务属性

    public Long getId() {
        return id;
    }
    // 返回数据库主键

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
    // 读取或修改启用状态
}