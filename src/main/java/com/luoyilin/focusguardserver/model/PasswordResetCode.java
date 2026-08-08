package com.luoyilin.focusguardserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "password_reset_codes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_password_reset_codes_user",
                        columnNames = "user_id"
                )
        }
)
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 数据库主键，由 MySQL 自动递增生成

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;
    // 验证码必须属于一个已经注册的用户

    @Column(
            name = "code_hash",
            nullable = false,
            length = 255
    )
    private String codeHash;
    // 只保存验证码的哈希值，不在数据库中保存明文验证码

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;
    // 保存验证码失效时间，超过该时间后不能继续使用

    @Column(
            name = "used",
            nullable = false
    )
    private boolean used = false;
    // 标记验证码是否已经使用，防止同一个验证码重复修改密码

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
    // 保存验证码记录的创建时间，并禁止后续修改

    public PasswordResetCode() {
    }
    // JPA 从数据库读取记录时需要无参数构造方法

    public PasswordResetCode(
            User user,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.used = false;
    }
    // 创建验证码记录时初始化所属用户、验证码哈希和失效时间

    @PrePersist
    private void beforeInsert() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    // 第一次写入数据库前自动记录创建时间

    public Long getId() {
        return id;
    }
    // 返回验证码记录的数据库主键

    public User getUser() {
        return user;
    }
    // 返回验证码所属用户

    public String getCodeHash() {
        return codeHash;
    }
    // 返回验证码哈希，供后端内部校验使用

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    // 返回验证码失效时间

    public boolean isUsed() {
        return used;
    }
    // 返回验证码是否已经使用

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    // 返回验证码记录创建时间

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    // 判断当前时间是否已经超过验证码失效时间

    public void markAsUsed() {
        this.used = true;
    }
    // 密码重置成功后把验证码标记为已使用
}
