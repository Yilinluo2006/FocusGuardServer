package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;

/** Account lock makes the five requests a shared, durable budget, including first insert. */
@Service
public class SleepTemporaryService {
    public record Request(@NotBlank @Pattern(regexp="[a-f0-9-]{36}") String requestId,
                          @Min(1) long sleepStart,@Min(1) long plannedEnd) { }
    public record State(long sleepStart,long plannedEnd,long serverTime,int left,String requestId,long validUntil) { }
    @Bean("sleepTemporaryClock") static Clock clock(){return Clock.systemUTC();}
    private final SleepTemporaryGrantRepository grants;
    private final UserRepository users;
    private final SharedSettingsRepository settings;
    private final Clock clock;
    private final ObjectMapper json=new ObjectMapper();
    public SleepTemporaryService(SleepTemporaryGrantRepository grants,UserRepository users,
            SharedSettingsRepository settings,@Qualifier("sleepTemporaryClock") Clock clock) {
        this.grants=grants;this.users=users;this.settings=settings;this.clock=clock;
    }
    private static ResponseStatusException error(HttpStatus code,String text){return new ResponseStatusException(code,text);}
    private Long owner(User user){if(user==null||user.getId()==null)throw error(HttpStatus.UNAUTHORIZED,"请先登录");return user.getId();}
    private void uuid(String id){try{if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException();}
        catch(RuntimeException e){throw error(HttpStatus.BAD_REQUEST,"请求编号无效");}}
    private long[] active(User user,long now){
        var config=settings.findById(owner(user)).orElse(null);
        if(config!=null){
            var sleep=json.readTree(config.getPayload()).path("sleep");
            if(sleep.path("daily").asBoolean()){
                LocalDate date=Instant.ofEpochMilli(now).atZone(DeviceSyncService.ZONE).toLocalDate();
                for(int days=0;days<=1;days++){
                    LocalDate candidate=date.minusDays(days);
                    if((sleep.path("sleepWeekdays").asInt()&(1<<(candidate.getDayOfWeek().getValue()-1)))==0)continue;
                    long start=candidate.atStartOfDay().plusMinutes(sleep.path("minute").asInt())
                            .atZone(DeviceSyncService.ZONE).toInstant().toEpochMilli();
                    long end=start+sleep.path("duration").asLong()*60_000L;
                    if(now>=start&&now<end)return new long[]{start,end};
                }
            }
        }
        throw error(HttpStatus.CONFLICT,"当前没有生效的共享睡眠计划");
    }
    private State state(long[] sleep,long now,List<SleepTemporaryGrant> rows,SleepTemporaryGrant grant){
        return new State(sleep[0],sleep[1],now,Math.max(0,5-rows.size()),
                grant==null?null:grant.getRequestId(),grant==null?0:grant.getValidUntil());
    }
    @Transactional(readOnly=true) public State get(User user,String device){
        uuid(device);long now=clock.millis();long[] sleep=active(user,now);
        var rows=grants.findAllByUserIdAndDay(owner(user),DeviceSyncService.quotaDay(Instant.ofEpochMilli(sleep[0])));
        return state(sleep,now,rows,null);
    }
    @Transactional public State grant(User user,String device,Request request){
        uuid(device);uuid(request.requestId());
        users.lockById(owner(user)).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"账号不存在"));
        long now=clock.millis();long[] sleep=active(user,now);
        if(request.sleepStart()!=sleep[0]||request.plannedEnd()!=sleep[1])
            throw error(HttpStatus.CONFLICT,"睡眠计划已变化，请同步后重试");
        LocalDate day=DeviceSyncService.quotaDay(Instant.ofEpochMilli(sleep[0]));
        var rows=grants.findAllByUserIdAndDay(user.getId(),day);
        var retry=grants.findByUserIdAndRequestId(user.getId(),request.requestId()).orElse(null);
        if(retry!=null){
            if(!device.equals(retry.getDeviceId())||retry.getSleepStart()!=sleep[0]||retry.getPlannedEnd()!=sleep[1])
                throw error(HttpStatus.CONFLICT,"临时使用请求已失效");
            return state(sleep,now,rows,retry);
        }
        // A second tap on the same device resumes the existing interval without extending it.
        for(var row:rows)if(device.equals(row.getDeviceId())&&row.getSleepStart()==sleep[0]&&row.getValidUntil()>now)
            return state(sleep,now,rows,row);
        if(rows.size()>=5)throw error(HttpStatus.TOO_MANY_REQUESTS,"两端共用的 5 次临时使用已用完");
        var grant=new SleepTemporaryGrant(user.getId(),device,request.requestId(),day,sleep[0],sleep[1],Math.min(sleep[1],now+120_000L));
        grants.saveAndFlush(grant);rows.add(grant);
        return state(sleep,now,rows,grant);
    }
}
