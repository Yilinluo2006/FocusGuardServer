package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="device_compliance", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","device_id","quota_day"}))
public class DeviceCompliance {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="device_id",nullable=false,length=36) private String deviceId;
    @Column(name="quota_day",nullable=false) private LocalDate day;
    private boolean participated;
    @Column(nullable=false,length=12) private String state;
    private long observedThrough;
    private Instant updatedAt;
    public DeviceCompliance() { }
    public DeviceCompliance(Long user,String device,LocalDate day){userId=user;deviceId=device;this.day=day;}
    public void update(boolean active,String outcome,long through){
        if("failed".equals(outcome)) {state="failed";participated|=active;observedThrough=Math.max(observedThrough,through);updatedAt=Instant.now();return;}
        if(through<observedThrough || through==observedThrough&&"complete".equals(state))return;
        participated=participated||active;
        // A confirmed violation is permanent. Old retries cannot turn it into a success.
        if(!"failed".equals(state))state="inactive".equals(outcome)&&participated?"pending":outcome;
        observedThrough=through;updatedAt=Instant.now();
    }
    public String getDeviceId(){return deviceId;}
    public LocalDate getDay(){return day;}
    public boolean isParticipated(){return participated;}
    public String getState(){return state;}
    public long getObservedThrough(){return observedThrough;}
}
