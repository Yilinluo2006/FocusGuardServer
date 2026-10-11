package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "email_binding_challenges", uniqueConstraints = @UniqueConstraint(name = "uk_binding_user", columnNames = "user_id"))
public class EmailBindingChallenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 100) private String email;
    @Column(nullable = false, length = 255) private String codeHash;
    @Column(nullable = false, length = 255) private String credentialHash;
    @Column(nullable = false) private Instant requestedAt;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int attempts;
    @Column(nullable = false) private boolean used;
    @Column(length=255) private String newPasswordHash;
    @Column(length=16) private String originalPhone;
    protected EmailBindingChallenge() { }
    public EmailBindingChallenge(User user) { this.user = user; }
    public void renew(String email, String codeHash, String passwordHash) {
        this.email = email; this.codeHash = codeHash; credentialHash = passwordHash == null ? "" : passwordHash;
        newPasswordHash=null; originalPhone=null;
        requestedAt = Instant.now(); expiresAt = requestedAt.plusSeconds(600); attempts = 0; used = false;
    }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public String getCredentialHash() { return credentialHash; }
    public void newPassword(String hash,String phone) { newPasswordHash=hash; originalPhone=phone; }
    public String getNewPasswordHash() { return newPasswordHash; }
    public String getOriginalPhone() { return originalPhone; }
    public Instant getRequestedAt() { return requestedAt; }
    public int getAttempts() { return attempts; }
    public boolean available() { return !used && attempts < 5 && Instant.now().isBefore(expiresAt); }
    public void attempt() { attempts++; }
    public void consume() { used = true; }
}
