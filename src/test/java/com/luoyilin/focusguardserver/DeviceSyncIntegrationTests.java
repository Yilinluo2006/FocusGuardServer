package com.luoyilin.focusguardserver;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.UserRepository;
import com.luoyilin.focusguardserver.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class DeviceSyncIntegrationTests {
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired JwtService jwt;
    @Autowired DeviceSyncService sync;
    @Autowired com.luoyilin.focusguardserver.repository.DeviceDayUsageRepository usageRows;
    @Autowired com.luoyilin.focusguardserver.repository.SharedSettingsRepository settingRows;
    private final List<User> created=new ArrayList<>();
    private final ObjectMapper json=new ObjectMapper();private String owner,other,phone,pad;
    private static final String SETTINGS="{\"tasks\":[],\"limits\":[{\"packageName\":\"com.example.video\",\"minutes\":30}],\"sleep\":{\"daily\":false,\"minute\":1410,\"duration\":240,\"sleepWeekdays\":127,\"allowed\":[]}}";
    @BeforeEach void setup()throws Exception{owner=account();other=account();phone=UUID.randomUUID().toString();pad=UUID.randomUUID().toString();settings(owner,0,SETTINGS).andExpect(status().isOk());}
    private String account(){String name="sync_"+UUID.randomUUID();User u=new User(name,name+"@example.invalid","unused");u.verifyEmail();users.saveAndFlush(u);created.add(u);return "Bearer "+jwt.generateToken(u);}
    private org.springframework.test.web.servlet.ResultActions settings(String auth,long version,String payload)throws Exception{return mvc.perform(put("/api/sync/settings").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(Map.of("expectedRevision",version,"payload",payload))));}
    private String usage(String auth,String device,long used,long want)throws Exception{
        var request=Map.of("deviceName",device.equals(phone)?"phone":"pad","deviceType",device.equals(phone)?"phone":"tablet","day",DeviceSyncService.quotaDay(Instant.now()).toString(),
                "usage",List.of(Map.of("packageName","com.example.video","name","video","millis",used)),"desiredCeilings",Map.of("com.example.video",want));
        return mvc.perform(post("/api/sync/usage/"+device).header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    private long ceiling(String response){return json.readTree(response).path("ceilings").path("com.example.video").asLong();}
    @Test void phoneTwentyLeavesPadTenAndRetriesCannotAllocateTwice()throws Exception{
        assertEquals(1200000,ceiling(usage(owner,phone,1200000,1200000)));
        assertEquals(600000,ceiling(usage(owner,pad,0,600000)));
        assertEquals(600000,ceiling(usage(owner,pad,0,600000)));
        assertEquals(1200000,ceiling(usage(owner,phone,1200000,1800000)));
    }
    @Test void outOfOrderCountersNeverDecreaseAndDevicesSum()throws Exception{
        usage(owner,phone,600000,720000);usage(owner,pad,300000,420000);
        var result=json.readTree(usage(owner,phone,100000,220000));
        assertEquals(900000,result.path("totalMillis").path("com.example.video").asLong());assertEquals(2,result.path("devices").size());
    }
    @Test void settingsConflictsAndOwnerIsolation()throws Exception{
        settings(owner,0,SETTINGS.replace("1410","1400")).andExpect(status().isConflict());
        mvc.perform(get("/api/sync/settings").header("Authorization",other)).andExpect(jsonPath("$.revision").value(0));
        var response=json.readTree(usage(other,pad,0,600000));assertEquals(0,response.path("ceilings").path("com.example.video").asLong());
        mvc.perform(get("/api/sync/settings")).andExpect(status().isUnauthorized());
    }
    @Test void fourAmBoundaryAndPastDayCannotAllocate()throws Exception{
        assertEquals(LocalDate.of(2026,10,3),DeviceSyncService.quotaDay(Instant.parse("2026-10-03T19:59:59Z")));
        assertEquals(LocalDate.of(2026,10,4),DeviceSyncService.quotaDay(Instant.parse("2026-10-03T20:00:00Z")));
        var request=Map.of("deviceName","phone","deviceType","phone","day",DeviceSyncService.quotaDay(Instant.now()).minusDays(1).toString(),"usage",List.of(),"desiredCeilings",Map.of("com.example.video",600000));
        String r=mvc.perform(post("/api/sync/usage/"+phone).header("Authorization",owner).contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertEquals(0,ceiling(r));
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void simultaneousDevicesCannotReserveTheSameBudget()throws Exception{
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var start=new java.util.concurrent.CountDownLatch(1);
        try{
            var user=created.get(0);var request=new com.luoyilin.focusguardserver.dto.DeviceSyncData.UsageRequest("concurrent-device","phone",DeviceSyncService.quotaDay(Instant.now()),List.of(),Map.of("com.example.video",1800000L));
            var first=pool.submit(()->{start.await();return sync.exchange(user,phone,request).ceilings().getOrDefault("com.example.video",0L);});
            var second=pool.submit(()->{start.await();return sync.exchange(user,pad,request).ceilings().getOrDefault("com.example.video",0L);});start.countDown();
            assertEquals(1800000L,first.get(8,java.util.concurrent.TimeUnit.SECONDS)+second.get(8,java.util.concurrent.TimeUnit.SECONDS));
        }finally{pool.shutdownNow();for(User user:created){usageRows.deleteAll(usageRows.findAllByUserIdAndDay(user.getId(),DeviceSyncService.quotaDay(Instant.now())));settingRows.deleteById(user.getId());users.deleteById(user.getId());}}
    }
}
