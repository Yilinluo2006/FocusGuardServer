package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Verified provider identity -> stable account. Never bind identities supplied without provider verification. */
@Entity
@Table(name = "account_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_identity_subject", columnNames = {"provider", "issuer", "subject"}),
        @UniqueConstraint(name = "uk_identity_account", columnNames = {"user_id", "provider", "issuer"})})
public class AccountIdentity {
    public enum Provider { EMAIL, PHONE, WECHAT }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Provider provider;
    @Column(nullable = false, length = 64, columnDefinition = "varchar(64) collate utf8mb4_bin") private String issuer;
    @Column(nullable = false, length = 191, columnDefinition = "varchar(191) collate utf8mb4_bin") private String subject;
    @Column(nullable = false) private Instant verifiedAt;
    protected AccountIdentity() { }
    public AccountIdentity(User user, Provider provider, String issuer, String subject) {
        this.user = user; this.provider = provider; this.issuer = issuer; this.subject = subject; verifiedAt = Instant.now();
    }
    public User getUser() { return user; }
    public Provider getProvider() { return provider; }
    public String getIssuer() { return issuer; }
    public String getSubject() { return subject; }
    public void replaceVerifiedSubject(String subject) { this.subject = subject; verifiedAt = Instant.now(); }
}
