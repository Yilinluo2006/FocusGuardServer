package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "phone_challenges", indexes = @Index(name = "ix_phone_challenge_owner", columnList = "phone,purpose,owner_id"))
public class PhoneChallenge {
    public enum Purpose { LOGIN, BIND, PROOF }
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 11) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Purpose purpose;
    @Column(name = "owner_id") private Long ownerId;
    @Column(nullable = false, length = 20) private String scheme;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int attempts;
    @Column(nullable = false) private boolean used;
    @Column(length = 255) private String credentialHash;
    @Column(length = 16) private String originalPhone;
    protected PhoneChallenge() { }
    public PhoneChallenge(String phone, Purpose purpose, User owner) {
        id = UUID.randomUUID().toString(); this.phone = phone; this.purpose = purpose;
        scheme = "FG" + purpose.name().charAt(0) + "_" + id.replace("-", "").substring(0, 12);
        expiresAt = Instant.now().plusSeconds(300);
        if (owner != null) { ownerId = owner.getId(); credentialHash = owner.getPasswordHash(); originalPhone = owner.getPhone(); }
    }
    public String getId() { return id; }
    public String getPhone() { return phone; }
    public Purpose getPurpose() { return purpose; }
    public Long getOwnerId() { return ownerId; }
    public String getScheme() { return scheme; }
    public boolean available() { return !used && attempts < 5 && Instant.now().isBefore(expiresAt); }
    public boolean matchesOwner(User owner) {
        return java.util.Objects.equals(ownerId, owner.getId()) && java.util.Objects.equals(credentialHash, owner.getPasswordHash())
                && java.util.Objects.equals(originalPhone, owner.getPhone());
    }
    public void attempt() { attempts++; }
    public void consume() { used = true; }
    public int getAttempts() { return attempts; }
}
