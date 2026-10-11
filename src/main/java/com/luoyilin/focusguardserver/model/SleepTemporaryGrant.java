package com.luoyilin.focusguardserver.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="sleep_temporary_grants", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_id"}),
        indexes=@Index(columnList="user_id,quota_day"))
public class SleepTemporaryGrant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="request_id",nullable=false,length=36) private String requestId;
    @Column(nullable=false,length=36) private String deviceId;
    @Column(name="quota_day",nullable=false) private LocalDate day;
    private long sleepStart, plannedEnd, validUntil;
    public SleepTemporaryGrant() { }
    public SleepTemporaryGrant(Long user,String device,String request,LocalDate day,long start,long end,long until) {
        userId=user;deviceId=device;requestId=request;this.day=day;sleepStart=start;plannedEnd=end;validUntil=until;
    }
    public String getRequestId(){return requestId;}
    public String getDeviceId(){return deviceId;}
    public long getSleepStart(){return sleepStart;}
    public long getPlannedEnd(){return plannedEnd;}
    public long getValidUntil(){return validUntil;}
}
