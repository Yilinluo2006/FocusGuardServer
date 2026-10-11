package com.luoyilin.focusguardserver.dto;

import jakarta.validation.constraints.*;

public record BackupRequest(
        @NotBlank @Size(max = 80) String deviceName,
        @NotNull @Min(0) Long expectedRevision,
        @NotBlank @Size(max = 2_000_000) String payload) { }
