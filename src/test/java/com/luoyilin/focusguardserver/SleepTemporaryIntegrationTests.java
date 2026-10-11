package com.luoyilin.focusguardserver;

import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import com.luoyilin.focusguardserver.service.*;
import com.luoyilin.focusguardserver.dto.DeviceSyncData.SettingsRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class SleepTemporaryIntegrationTests {
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired JwtService jwt;
    @Autowired DeviceSyncService settings;@Autowired SleepTemporaryService sleep;
    @Autowired SleepTemporaryGrantRepository grants;@Autowired SharedSettingsRepository configs;
    @MockitoBean(name="sleepTemporaryClock") Clock clock;
    private User user,other;private String auth,otherAuth,phone,pad;private long start,end,now;
    private final ObjectMapper json=new ObjectMapper();
    @BeforeEach void setup(){
        user=account();other=account();auth="Bearer "+jwt.generateToken(user);otherAuth="Bearer "+jwt.generateToken(other);
        phone=UUID.randomUUID().toString();pad=UUID.randomUUID().toString();
        start=Instant.parse("2026-10-03T15:30:00Z").toEpochMilli();end=start+720*60_000L;now=start+60_000;
        time(now);
        settings.put(user,new SettingsRequest(0,"{\"tasks\":[],\"limits\":[],\"sleep\":{\"daily\":true,\"minute\":1410,\"duration\":720,\"sleepWeekdays\":127,\"allowed\":[]}}"));
    }
    private User account(){String n="sleep_"+UUID.randomUUID();User u=new User(n,n+"@example.invalid","unused");u.verifyEmail();return users.saveAndFlush(u);}
    private void time(long at){when(clock.millis()).thenReturn(at);}
    private SleepTemporaryService.Request request(String id){return new SleepTemporaryService.Request(id,start,end);}
    private org.springframework.test.web.servlet.ResultActions grant(String token,String device,String id)throws Exception{
        return mvc.perform(post("/api/sync/sleep/temporary/"+device).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(request(id))));
    }
    @Test void fiveRequestsAreSharedAndSixthIsDenied()throws Exception{
        for(int i=0;i<5;i++){time(now+i*121_000L);grant(auth,i%2==0?phone:pad,UUID.randomUUID().toString()).andExpect(status().isOk()).andExpect(jsonPath("$.left").value(4-i));}
        time(now+5*121_000L);grant(auth,pad,UUID.randomUUID().toString()).andExpect(status().isTooManyRequests());
        mvc.perform(get("/api/sync/sleep/temporary/"+phone).header("Authorization",auth)).andExpect(jsonPath("$.left").value(0));
    }
    @Test void retryAndDoubleTapCannotSpendTwiceOrExtendTheDeadline()throws Exception{
        String id=UUID.randomUUID().toString();var first=sleep.grant(user,phone,request(id));assertEquals(4,first.left());assertEquals(now+120_000,first.validUntil());
        time(now+30_000);var retry=sleep.grant(user,phone,request(id));assertEquals(first.validUntil(),retry.validUntil());assertEquals(4,retry.left());
        var doubleTap=sleep.grant(user,phone,request(UUID.randomUUID().toString()));assertEquals(first.validUntil(),doubleTap.validUntil());assertEquals(4,doubleTap.left());
        time(now+121_000);var late=sleep.grant(user,phone,request(id));assertTrue(late.validUntil()<late.serverTime());assertEquals(4,late.left());
        grant(auth,pad,id).andExpect(status().isConflict());
    }
    @Test void fourAmDoesNotReplenishAnOngoingSleepAndNextNightDoes()throws Exception{
        for(int i=0;i<5;i++){time(now+i*121_000L);sleep.grant(user,i%2==0?phone:pad,request(UUID.randomUUID().toString()));}
        time(Instant.parse("2026-10-03T20:01:00Z").toEpochMilli());assertEquals(0,sleep.get(user,pad).left());
        grant(auth,phone,UUID.randomUUID().toString()).andExpect(status().isTooManyRequests());
        time(start+24*3600_000L+60_000);assertEquals(5,sleep.get(user,pad).left());
    }
    @Test void authenticationPlanAndOwnerAreCheckedAndEndTruncatesTheGrant()throws Exception{
        mvc.perform(get("/api/sync/sleep/temporary/"+phone)).andExpect(status().isUnauthorized());
        grant(otherAuth,pad,UUID.randomUUID().toString()).andExpect(status().isConflict());
        mvc.perform(post("/api/sync/sleep/temporary/"+phone).header("Authorization",auth).contentType("application/json")
                .content(json.writeValueAsString(new SleepTemporaryService.Request(UUID.randomUUID().toString(),start+1,end)))).andExpect(status().isConflict());
        time(end-30_000);assertEquals(end,sleep.grant(user,phone,request(UUID.randomUUID().toString())).validUntil());
        time(end);grant(auth,pad,UUID.randomUUID().toString()).andExpect(status().isConflict());
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentPhoneAndPadCannotBothTakeTheFifthUse()throws Exception{
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var gate=new java.util.concurrent.CountDownLatch(1);
        try{
            for(int i=0;i<4;i++){time(now+i*121_000L);sleep.grant(user,phone,request(UUID.randomUUID().toString()));}
            time(now+4*121_000L);
            java.util.concurrent.Callable<Boolean> a=()->{gate.await();try{sleep.grant(user,phone,request(UUID.randomUUID().toString()));return true;}
                catch(org.springframework.web.server.ResponseStatusException e){assertEquals(429,e.getStatusCode().value());return false;}};
            java.util.concurrent.Callable<Boolean> b=()->{gate.await();try{sleep.grant(user,pad,request(UUID.randomUUID().toString()));return true;}
                catch(org.springframework.web.server.ResponseStatusException e){assertEquals(429,e.getStatusCode().value());return false;}};
            var f=pool.submit(a);var g=pool.submit(b);gate.countDown();assertNotEquals(f.get(8,java.util.concurrent.TimeUnit.SECONDS),g.get(8,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0,sleep.get(user,phone).left());
        }finally{pool.shutdownNow();grants.deleteAll(grants.findAllByUserIdAndDay(user.getId(),DeviceSyncService.quotaDay(Instant.ofEpochMilli(start))));
            configs.deleteById(user.getId());users.deleteById(user.getId());users.deleteById(other.getId());}
    }
}
