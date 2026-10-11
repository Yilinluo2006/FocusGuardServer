package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public final class DeviceSyncData {
    public record Settings(long revision,String payload,Instant updatedAt,String timezone) { }
    public record SettingsRequest(@Min(0) long expectedRevision,@NotBlank @Size(max=1_000_000) String payload) { }
    public record UsageItem(@NotBlank @Pattern(regexp="[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+") String packageName,
                            @NotBlank @Size(max=120) String name,@Min(0) @Max(93_600_000) long millis) { }
    public record UsageRequest(@NotBlank @Size(max=80) String deviceName,
                               @NotBlank @Pattern(regexp="phone|tablet") String deviceType,
                               @NotNull LocalDate day,
                               @NotNull @Size(max=2000) List<@jakarta.validation.Valid UsageItem> usage,
                               @NotNull @Size(max=2000) Map<String,Long> desiredCeilings) { }
    public record DeviceUsage(String deviceId,String deviceName,String deviceType,Instant updatedAt,List<UsageItem> usage) { }
    public record UsageResult(LocalDate day,long serverTime,long validUntil,Map<String,Long> ceilings,
                              Map<String,Long> totalMillis,List<DeviceUsage> devices) { }
}
