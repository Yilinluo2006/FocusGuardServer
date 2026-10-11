package com.luoyilin.focusguardserver.model;
import jakarta.persistence.*;
@Entity @Table(name = "sms_throttles")
public class SmsThrottle {
    @Id @Column(name = "scope_key", length = 100) private String scopeKey;
    @Column(name = "requested_at", nullable = false) private long requestedAt;
    @Column(name = "window_started_at", nullable = false) private long windowStartedAt;
    @Column(name = "request_count", nullable = false) private int requestCount;
    public boolean reserve(long now, long window, long interval, int limit) {
        if (now - windowStartedAt >= window) { windowStartedAt = now; requestCount = 0; }
        if (now - requestedAt < interval || requestCount >= limit) return false;
        requestedAt = now; requestCount++; return true;
    }
}
