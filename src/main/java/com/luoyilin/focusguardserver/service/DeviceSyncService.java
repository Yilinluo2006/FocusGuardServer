package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.dto.DeviceSyncData.*;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;

/** All writes lock the account, including first inserts and quota allocation. */
@Service
public class DeviceSyncService {
    public static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    private final SharedSettingsRepository settings;
    private final DeviceDayUsageRepository usage;
    private final UserRepository users;
    private final ObjectMapper json=new ObjectMapper();
    public DeviceSyncService(SharedSettingsRepository s,DeviceDayUsageRepository u,UserRepository users){settings=s;usage=u;this.users=users;}
    public static LocalDate quotaDay(Instant now){return now.atZone(ZONE).minusHours(4).toLocalDate();}
    private Long owner(User user){if(user==null||user.getId()==null)throw error(HttpStatus.UNAUTHORIZED,"请先登录");return user.getId();}
    private void lock(User user){users.lockById(owner(user)).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"账号不存在"));}
    private static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
    private void device(String id){try{if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException();}catch(RuntimeException e){throw error(HttpStatus.BAD_REQUEST,"设备编号无效");}}
    @Transactional(readOnly=true) public Settings get(User user){
        var value=settings.findById(owner(user)).orElse(null);
        return value==null?new Settings(0,null,null,ZONE.getId()):new Settings(value.getRevision(),value.getPayload(),value.getUpdatedAt(),ZONE.getId());
    }
    @Transactional public Settings put(User user,SettingsRequest request){
        validateSettings(request.payload()); lock(user);
        SharedSettings value=settings.findById(user.getId()).orElse(null);
        if(value!=null&&value.getPayload().equals(request.payload()))return get(user);
        if((value==null?0:value.getRevision())!=request.expectedRevision())throw error(HttpStatus.CONFLICT,"另一台设备已修改设置");
        if(value==null)value=new SharedSettings(user.getId());
        value.update(request.payload());settings.saveAndFlush(value);return get(user);
    }
    private void validateSettings(String payload){
        try{
            var root=json.readTree(payload);
            if(!root.isObject()||root.size()!=3||!root.path("tasks").isArray()||!root.path("sleep").isObject()||!root.path("limits").isArray()
                    ||root.path("tasks").size()>2000||root.path("limits").size()>2000)throw new IllegalArgumentException();
            Set<String> ids=new HashSet<>();
            for(var task:root.path("tasks")){
                String mode=task.path("mode").asText();int v=task.path("value").asInt(-1);
                if(task.path("id").asText().isBlank()||!ids.add(task.path("id").asText())||task.path("name").asText().isBlank()
                        ||!(mode.equals("countup")&&v==0||mode.equals("countdown")&&v>=1&&v<=720||mode.equals("pomodoro")&&v>=1&&v<=24))throw new IllegalArgumentException();
            }
            ids.clear();for(var limit:root.path("limits")){
                String pkg=limit.path("packageName").asText();int min=limit.path("minutes").asInt();
                if(!pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")||!ids.add(pkg)||min<1||min>1440)throw new IllegalArgumentException();
            }
            var sleep=root.path("sleep"); int minute=sleep.path("minute").asInt(-1),duration=sleep.path("duration").asInt(),days=sleep.path("sleepWeekdays").asInt(-1);
            if(sleep.size()!=5||!sleep.path("daily").isBoolean()||minute<0||minute>=1440||duration<1||duration>720||days<0||days>127
                    ||sleep.path("daily").asBoolean()&&days==0||!sleep.path("allowed").isArray()||sleep.path("allowed").size()>2000)throw new IllegalArgumentException();
            for(var pkg:sleep.path("allowed"))if(!pkg.asText().matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))throw new IllegalArgumentException();
        }catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"同步设置格式无效");}
    }
    private List<UsageItem> items(DeviceDayUsage row){
        return Arrays.asList(json.readValue(row.getUsagePayload(),UsageItem[].class));
    }
    private Map<String,Long> grants(DeviceDayUsage row){
        Map<String,Long> result=new TreeMap<>();json.readTree(row.getCeilings()).properties().forEach(e->result.put(e.getKey(),e.getValue().asLong()));return result;
    }
    @Transactional public UsageResult exchange(User user,String device,UsageRequest request){
        device(device); lock(user);LocalDate today=quotaDay(Instant.now());
        if(request.day().isAfter(today)||request.day().isBefore(today.minusDays(366)))throw error(HttpStatus.BAD_REQUEST,"使用记录日期无效");
        Map<String,UsageItem> incoming=new TreeMap<>();
        for(UsageItem item:request.usage())if(incoming.put(item.packageName(),item)!=null)throw error(HttpStatus.BAD_REQUEST,"重复的应用记录");
        DeviceDayUsage row=usage.findByUserIdAndDeviceIdAndDay(user.getId(),device,request.day()).orElse(new DeviceDayUsage(user.getId(),device,request.day()));
        for(UsageItem old:items(row)){
            UsageItem changed=incoming.get(old.packageName());
            if(changed==null||changed.millis()<old.millis())incoming.put(old.packageName(),old);
        }
        if(incoming.size()>2000)throw error(HttpStatus.BAD_REQUEST,"使用记录过多");
        Map<String,Long> allocated=grants(row);
        Map<String,Long> reserved=new HashMap<>();
        for(DeviceDayUsage other:usage.findAllByUserIdAndDay(user.getId(),request.day())){
            if(device.equals(other.getDeviceId()))continue;
            Map<String,Long> held=grants(other);
            for(UsageItem item:items(other))held.merge(item.packageName(),item.millis(),Math::max);
            held.forEach((pkg,ms)->reserved.merge(pkg,ms,Long::sum));
        }
        for(UsageItem item:incoming.values())allocated.merge(item.packageName(),item.millis(),Math::max);
        Map<String,Long> limits=new HashMap<>();
        var shared=settings.findById(user.getId()).orElse(null);
        if(shared!=null)for(var limit:json.readTree(shared.getPayload()).path("limits"))limits.put(limit.path("packageName").asText(),limit.path("minutes").asLong()*60000);
        if(request.day().equals(today))for(var want:request.desiredCeilings().entrySet()){
            if(want.getValue()==null||want.getValue()<0||want.getValue()>93_600_000L)throw error(HttpStatus.BAD_REQUEST,"额度请求无效");
            if(!limits.containsKey(want.getKey()))continue;
            long held=allocated.getOrDefault(want.getKey(),0L);
            // Absolute targets make retries idempotent; never reclaim an offline device's grant.
            long remaining=Math.max(0,limits.get(want.getKey())-reserved.getOrDefault(want.getKey(),0L)-held);
            allocated.put(want.getKey(),held+Math.min(remaining,Math.max(0,want.getValue()-held)));
        }
        row.update(request.deviceName(),request.deviceType(),json.writeValueAsString(incoming.values()),json.writeValueAsString(allocated));
        usage.saveAndFlush(row);
        return result(user,request.day(),device);
    }
    @Transactional(readOnly=true) public UsageResult result(User user,LocalDate day,String device){
        owner(user); device(device);if(day==null)day=quotaDay(Instant.now());
        Map<String,Long> total=new TreeMap<>(),ceilings=new TreeMap<>();List<DeviceUsage> devices=new ArrayList<>();
        for(var row:usage.findAllByUserIdAndDay(user.getId(),day)){
            var list=items(row);list.forEach(i->total.merge(i.packageName(),i.millis(),Long::sum));
            if(device.equals(row.getDeviceId()))ceilings.putAll(grants(row));
            devices.add(new DeviceUsage(row.getDeviceId(),row.getDeviceName(),row.getDeviceType(),row.getUpdatedAt(),list));
        }
        long until=day.plusDays(1).atTime(4,0).atZone(ZONE).toInstant().toEpochMilli();
        return new UsageResult(day,Instant.now().toEpochMilli(),until,ceilings,total,devices);
    }
}
