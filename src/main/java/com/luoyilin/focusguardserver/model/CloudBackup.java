package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cloud_backups", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
public class CloudBackup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "device_id", nullable = false, length = 36)
    private String deviceId;
    @Column(nullable = false, length = 80)
    private String deviceName;
    @Column(nullable = false)
    private long revision;
    @Column(nullable = false)
    private Instant updatedAt;
    @Column(nullable = false, length = 64)
    private String hash;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;
    public CloudBackup() { }
    public CloudBackup(User user, String deviceId) { this.user = user; this.deviceId = deviceId; }
    public void update(String name, String payload, String hash) {
        this.deviceName = name; this.payload = payload; this.hash = hash; revision++; updatedAt = Instant.now();
    }
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public long getRevision() { return revision; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getHash() { return hash; }
    public String getPayload() { return payload; }
}
