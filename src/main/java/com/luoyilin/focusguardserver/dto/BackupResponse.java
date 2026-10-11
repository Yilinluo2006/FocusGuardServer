package com.luoyilin.focusguardserver.dto;

import com.luoyilin.focusguardserver.model.CloudBackup;
import java.time.Instant;

public record BackupResponse(String deviceId, String deviceName, long revision, Instant updatedAt, String hash, String payload) {
    public static BackupResponse from(CloudBackup value, boolean includePayload) {
        return new BackupResponse(value.getDeviceId(), value.getDeviceName(), value.getRevision(), value.getUpdatedAt(),
                value.getHash(), includePayload ? value.getPayload() : null);
    }
}
