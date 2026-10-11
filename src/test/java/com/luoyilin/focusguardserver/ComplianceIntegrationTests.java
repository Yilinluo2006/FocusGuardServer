package com.luoyilin.focusguardserver;
import com.luoyilin.focusguardserver.dto.ComplianceData.*;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.UserRepository;
import com.luoyilin.focusguardserver.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional class ComplianceIntegrationTests {
    @Autowired ComplianceService service;@Autowired UserRepository users;@Autowired DeviceSyncService sync;
    private User owner,other;private final String phone=UUID.randomUUID().toString(),pad=UUID.randomUUID().toString();
    private final LocalDate day=DeviceSyncService.quotaDay(Instant.now()).minusDays(1);
    private final long through=day.plusDays(1).atTime(4,0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    @BeforeEach void setup(){owner=account();other=account();}
    private User account(){String id="compliance_"+UUID.randomUUID();User u=new User(id,id+"@example.invalid","unused");u.verifyEmail();return users.saveAndFlush(u);}
    private String report(User u,String device,boolean active,String outcome,long time){return service.exchange(u,device,new Request(List.of(new Day(day,active,outcome,time)))).days().get(0).state();}
    @Test void unusedPadIsIgnoredButAnActivePendingPadMustConfirm(){
        assertEquals("complete",report(owner,phone,true,"complete",through));
        assertEquals("complete",report(owner,pad,false,"inactive",through));
        assertEquals("pending",report(owner,pad,true,"pending",through));
        assertEquals("complete",report(owner,pad,true,"complete",through));
    }
    @Test void violationSurvivesRetriesAndAnotherAccountCannotAffectIt(){
        assertEquals("complete",report(owner,phone,true,"complete",through));
        assertEquals("failed",report(owner,pad,true,"failed",through));
        assertEquals("failed",report(owner,pad,true,"complete",through));
        assertEquals("complete",report(other,pad,true,"complete",through));
    }
    @Test void oldMiddayRequestCannotReplaceAFinalizedRecord(){
        assertEquals("complete",report(owner,phone,true,"complete",through));
        assertEquals("complete",report(owner,phone,true,"pending",through-60000));
        assertEquals("complete",report(owner,phone,true,"pending",through));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->report(owner,pad,true,"complete",through-1));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->report(owner,pad,false,"complete",through));
    }
    @Test void anUnusedPadMiddayReportCannotConfirmTheWholeNight(){
        report(owner,phone,true,"complete",through);
        assertEquals("pending",report(owner,pad,false,"inactive",through-60000));
        assertEquals("complete",report(owner,pad,false,"inactive",through));
    }
    @Test void actualUsageFromAnOlderClientCreatesUncertaintyNotFailure(){
        sync.exchange(owner,pad,new com.luoyilin.focusguardserver.dto.DeviceSyncData.UsageRequest("pad","tablet",day,
                List.of(new com.luoyilin.focusguardserver.dto.DeviceSyncData.UsageItem("com.example.video","video",900000)),Map.of()));
        assertEquals("pending",report(owner,phone,true,"complete",through));
        assertEquals("complete",report(owner,pad,true,"complete",through));
    }
}
