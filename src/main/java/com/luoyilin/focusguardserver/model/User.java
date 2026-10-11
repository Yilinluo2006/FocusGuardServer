package com.luoyilin.focusguardserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_username",
                        columnNames = "username"
                ),
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uk_users_phone",
                        columnNames = "phone"
                )
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // MySQL 自动生成的用户主键

    @Column(
            name = "username",
            nullable = false,
            length = 50
    )
    private String username;
    // Immutable legacy handle stays unique; display names may repeat.
    @Column(name = "display_name", length = 96)
    private String displayName;
    @Column(name = "avatar_key", length = 32)
    private String avatarKey;
    // 用户在 FocusGuard 中显示的名称

    @Column(
            name = "email",
            length = 100
    )
    private String email;
    // 用户登录和找回密码时使用的唯一邮箱

    @Column(
            name = "password_hash",
            length = 255
    )
    private String passwordHash;
    // 只保存 BCrypt 处理后的密码哈希，不保存明文密码

    @Column(
            name = "email_verified",
            nullable = false
    )
    private boolean emailVerified = false;
    @Column(length = 16) private String phone;
    @Column(nullable = false) private boolean phoneVerified = false;
    @Column(nullable = false, columnDefinition = "bit(1) not null default b'0'") private boolean wechatVerified = false;
    // 表示邮箱是否已经通过验证码验证，默认未验证

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
    // 保存账号创建时间，创建后不允许通过 JPA 修改

    public User() {
    }
    // JPA 创建实体对象时需要无参构造方法

    public User(
            String username,
            String email,
            String passwordHash
    ) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = false;
    }
    // 创建新用户时初始化用户名、邮箱和密码哈希

    @PrePersist
    private void beforeInsert() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    // 第一次保存到数据库前，自动记录当前时间

    public Long getId() {
        return id;
    }
    // 返回用户主键

    public String getUsername() {
        return username;
    }
    public static User createDefault(String email, String passwordHash) {
        User user = new User("fg_" + java.util.UUID.randomUUID().toString().replace("-", ""), email, passwordHash);
        user.displayName = "小奶娃"; user.avatarKey = "default";
        return user;
    }
    public String getDisplayName() { return displayName == null || displayName.isBlank() ? username : displayName; }
    public String getAvatarKey() { return avatarKey == null ? "default" : avatarKey; }
    public void updateProfile(String name, String avatar) { displayName = name; avatarKey = avatar; }
    public void bindEmail(String email) { this.email = email; this.emailVerified = true; }
    public static User createPhone(String phone) {
        User user = createDefault(null, null); user.bindPhone(phone); return user;
    }
    public void bindPhone(String phone) { this.phone = phone; phoneVerified = true; }
    public String getPhone() { return phone; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public boolean hasVerifiedLogin() { return (email != null && emailVerified) || (phone != null && phoneVerified) || wechatVerified; }
    public void verifyWechat() { wechatVerified = true; }
    public boolean isWechatVerified() { return wechatVerified; }
    // 返回用户名

    public String getEmail() {
        return email;
    }
    // 返回邮箱

    public String getPasswordHash() {
        return passwordHash;
    }
    // 返回密码哈希，仅供后端内部验证密码

    public boolean isEmailVerified() {
        return emailVerified;
    }
    // 返回邮箱是否已经验证

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    // 返回账号创建时间

    public void verifyEmail() {
        this.emailVerified = true;
    }
    // 邮箱验证码验证成功后，把状态改为已验证

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    // 密码重置成功后，用新的 BCrypt 哈希替换旧密码哈希
}
