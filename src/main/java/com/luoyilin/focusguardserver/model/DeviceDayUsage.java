package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="device_day_usage", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","device_id","quota_day"}))
public class DeviceDayUsage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="device_id",nullable=false,length=36) private String deviceId;
    @Column(name="quota_day",nullable=false) private LocalDate day;
    @Column(nullable=false,length=80) private String deviceName;
    @Column(nullable=false,length=10) private String deviceType;
    @Lob @Column(nullable=false,columnDefinition="LONGTEXT") private String usagePayload="[]";
    @Lob @Column(nullable=false,columnDefinition="LONGTEXT") private String ceilings="{}";
    private Instant updatedAt;
    public DeviceDayUsage() { }
    public DeviceDayUsage(Long user, String device, LocalDate day) { this.userId=user; this.deviceId=device; this.day=day; }
    public void update(String name,String type,String payload,String grants) {
        deviceName=name; deviceType=type; usagePayload=payload; ceilings=grants; updatedAt=Instant.now();
    }
    public String getDeviceId(){return deviceId;}
    public String getDeviceName(){return deviceName;}
    public String getDeviceType(){return deviceType;}
    public String getUsagePayload(){return usagePayload;}
    public String getCeilings(){return ceilings;}
    public Instant getUpdatedAt(){return updatedAt;}
}
