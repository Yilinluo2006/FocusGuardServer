package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.dto.ComplianceData.*;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service public class ComplianceService {
    private final DeviceComplianceRepository records;
    private final DeviceDayUsageRepository usage;
    private final UserRepository users;
    private static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    public ComplianceService(DeviceComplianceRepository records,DeviceDayUsageRepository usage,UserRepository users){this.records=records;this.usage=usage;this.users=users;}
    private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"守约记录格式无效");}
    @Transactional public Result exchange(User user,String device,Request request){
        try{if(!UUID.fromString(device).toString().equals(device))throw invalid();}catch(IllegalArgumentException e){throw invalid();}
        // The same account lock as quotas serializes simultaneous device reports.
        users.lockById(user.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        long now=System.currentTimeMillis();LocalDate today=DeviceSyncService.quotaDay(Instant.ofEpochMilli(now));
        Set<LocalDate> dates=new LinkedHashSet<>();
        for(Day day:request.days()){
            long from=day.day().atTime(4,0).atZone(ZONE).toInstant().toEpochMilli();
            long until=day.day().plusDays(1).atTime(4,0).atZone(ZONE).toInstant().toEpochMilli();
            if(!dates.add(day.day())||day.day().isAfter(today)||day.day().isBefore(today.minusDays(366))
                    ||day.observedThrough()>now+30_000||day.observedThrough()<from||day.observedThrough()>until
                    ||"complete".equals(day.state())&&(!day.participated()||day.observedThrough()<until)
                    ||"inactive".equals(day.state())&&day.participated())throw invalid();
            DeviceCompliance value=records.findByUserIdAndDeviceIdAndDay(user.getId(),device,day.day()).orElse(new DeviceCompliance(user.getId(),device,day.day()));
            value.update(day.participated(),day.state(),day.observedThrough());records.save(value);
        }
        records.flush();List<Day> result=new ArrayList<>();
        for(LocalDate date:dates){
            long until=date.plusDays(1).atTime(4,0).atZone(ZONE).toInstant().toEpochMilli();
            Map<String,DeviceCompliance> reports=new HashMap<>();for(var row:records.findAllByUserIdAndDay(user.getId(),date))reports.put(row.getDeviceId(),row);
            boolean active=false,pending=false,failed=false;
            for(var row:reports.values()){
                active|=row.isParticipated();failed|="failed".equals(row.getState());
                pending|="pending".equals(row.getState()) || !"failed".equals(row.getState())&&row.getObservedThrough()<until;
            }
            for(var row:usage.findAllByUserIdAndDay(user.getId(),date)){
                // Old clients with actual usage and no report are unverified, not automatically failed.
                if(!reports.containsKey(row.getDeviceId())&&!"[]".equals(row.getUsagePayload()))pending=true;
            }
            String outcome=failed?"failed":pending||now<until?"pending":active?"complete":"inactive";
            result.add(new Day(date,active,outcome,Math.min(now,until)));
        }
        return new Result(now,result);
    }
}
