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
        name = "email_verification_codes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_email_verification_codes_user",
                        columnNames = "user_id"
                )
        }
)
public class EmailVerificationCode {

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
    // 每条邮箱验证码都必须属于一个已经注册的用户

    @Column(
            name = "code_hash",
            nullable = false,
            length = 255
    )
    private String codeHash;
    // 数据库只保存验证码哈希，不保存邮件中的六位明文验证码

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;
    // 超过这个时间后，验证码不能继续使用

    @Column(
            name = "used",
            nullable = false
    )
    private boolean used = false;
    // 验证成功后标记为已使用，防止验证码重复提交

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
    // 保存验证码创建时间，用于判断六十秒发送冷却期

    public EmailVerificationCode() {
    }
    // JPA 从数据库读取实体时需要无参构造方法

    public EmailVerificationCode(
            User user,
            String codeHash,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.used = false;
    }
    // 创建验证码记录时保存所属用户、验证码哈希和失效时间

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
    // 返回验证码记录主键

    public User getUser() {
        return user;
    }
    // 返回验证码所属用户

    public String getCodeHash() {
        return codeHash;
    }
    // 返回验证码哈希，供后端内部进行 BCrypt 比对

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
    // 返回验证码创建时间

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    // 当前时间晚于失效时间时，说明验证码已经过期

    public void markAsUsed() {
        this.used = true;
    }
    // 邮箱验证成功后把验证码标记为已使用
}
