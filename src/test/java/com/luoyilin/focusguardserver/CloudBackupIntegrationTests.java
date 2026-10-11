package com.luoyilin.focusguardserver;

import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.UserRepository;
import com.luoyilin.focusguardserver.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CloudBackupIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    private final ObjectMapper mapper = new ObjectMapper();
    private String owner, other, device;
    private static final String PAYLOAD = "{\"schemaVersion\":1,\"limits\":[],\"tasks\":[],\"focus\":{}}";
    @BeforeEach void accounts() {
        owner = account(); other = account(); device = UUID.randomUUID().toString();
    }
    private String account() {
        String name = "backup_" + UUID.randomUUID();
        User user = new User(name, name + "@example.invalid", "unused-test-hash");
        user.verifyEmail(); users.saveAndFlush(user); return "Bearer " + jwt.generateToken(user);
    }
    private String request(long revision, String payload) {
        return mapper.writeValueAsString(java.util.Map.of("deviceName", "integration-device", "expectedRevision", revision, "payload", payload));
    }
    private org.springframework.test.web.servlet.ResultActions upload(String account, long revision, String payload) throws Exception {
        return mvc.perform(put("/api/backups/" + device).header("Authorization", account)
                .contentType("application/json").content(request(revision, payload)));
    }
    @Test void requiresLogin() throws Exception {
        mvc.perform(get("/api/backups")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/backups/" + device).contentType("application/json").content(request(0, PAYLOAD)))
                .andExpect(status().isUnauthorized());
    }
    @Test void persistsAndListsMetadataWithoutPayload() throws Exception {
        upload(owner, 0, PAYLOAD).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        mvc.perform(get("/api/backups/" + device).header("Authorization", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payload").value(PAYLOAD));
        var body = mvc.perform(get("/api/backups").header("Authorization", owner)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var list = mapper.readTree(body); assertEquals(1, list.size());
        assertTrue(list.get(0).path("payload").isNull() || list.get(0).path("payload").isMissingNode());
        assertEquals(64, list.get(0).path("hash").asText().length());
    }
    @Test void isolatesOwnersEvenForSameDevice() throws Exception {
        upload(owner, 0, PAYLOAD).andExpect(status().isOk());
        mvc.perform(get("/api/backups/" + device).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/backups").header("Authorization", other)).andExpect(jsonPath("$.length()").value(0));
        upload(other, 0, PAYLOAD).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
    }
    @Test void lostResponseRetryDoesNotCreateAnotherRevision() throws Exception {
        upload(owner, 0, PAYLOAD).andExpect(status().isOk());
        upload(owner, 0, PAYLOAD).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
    }
    @Test void staleUpdateIsRejectedAndOriginalPreserved() throws Exception {
        upload(owner, 0, PAYLOAD).andExpect(status().isOk());
        String changed = PAYLOAD.replace("\"tasks\":[]", "\"tasks\":[{\"id\":\"new\"}]");
        upload(owner, 0, changed).andExpect(status().isConflict());
        mvc.perform(get("/api/backups/" + device).header("Authorization", owner)).andExpect(jsonPath("$.payload").value(PAYLOAD));
        upload(owner, 1, changed).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2));
    }
    @Test void malformedAndFutureSchemasAreRejected() throws Exception {
        upload(owner, 0, "broken JSON").andExpect(status().isBadRequest());
        upload(owner, 0, PAYLOAD.replace(":1", ":2")).andExpect(status().isBadRequest());
        upload(owner, -1, PAYLOAD).andExpect(status().isBadRequest());
        mvc.perform(get("/api/backups/not-a-uuid").header("Authorization", owner)).andExpect(status().isBadRequest());
    }
    @Test void largeHistorySurvivesDatabaseRoundTrip() throws Exception {
        String large = PAYLOAD.replace("\"focus\":{}", "\"focus\":{\"history\":\"" + "中".repeat(100_000) + "\"}");
        upload(owner, 0, large).andExpect(status().isOk());
        String response = mvc.perform(get("/api/backups/" + device).header("Authorization", owner))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(large, mapper.readTree(response).path("payload").asText());
    }
}
