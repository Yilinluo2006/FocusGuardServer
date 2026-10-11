package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="shared_settings")
public class SharedSettings {
    @Id private Long userId;
    private long revision;
    @Lob @Column(columnDefinition="LONGTEXT", nullable=false) private String payload;
    private Instant updatedAt;
    public SharedSettings() { }
    public SharedSettings(Long userId) { this.userId=userId; }
    public void update(String value) { payload=value; revision++; updatedAt=Instant.now(); }
    public long getRevision() { return revision; }
    public String getPayload() { return payload; }
    public Instant getUpdatedAt() { return updatedAt; }
}
