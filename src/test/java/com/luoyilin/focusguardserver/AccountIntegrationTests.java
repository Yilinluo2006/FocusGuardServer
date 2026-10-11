package com.luoyilin.focusguardserver;

import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import com.luoyilin.focusguardserver.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountIdentityRepository identities;
    @Autowired EmailBindingChallengeRepository challenges;
    @Autowired PasswordResetCodeRepository resetCodes;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager entityManager;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @MockitoBean EmailService mail;
    private final ObjectMapper json = new ObjectMapper();
    private User user;
    private String auth, target;
    private static final String PASSWORD = "test-password-2026";
    private static final String PROFILE = "/api/account/profile";
    private static final String REQUEST = "/api/account/bindings/email/request";
    private static final String CONFIRM = "/api/account/bindings/email/confirm";

    @BeforeEach void account() {
        String email = address();
        user = new User("legacy_" + UUID.randomUUID(), email, passwords.encode(PASSWORD));
        user.verifyEmail(); users.saveAndFlush(user);
        auth = "Bearer " + jwt.generateToken(user); target = address();
    }
    private String address() { return UUID.randomUUID() + "@example.invalid"; }
    private String body(Map<String, ?> data) { return json.writeValueAsString(data); }
    private org.springframework.test.web.servlet.ResultActions request(String password, String email) throws Exception {
        return mvc.perform(post(REQUEST).header("Authorization", auth).contentType("application/json")
                .content(body(Map.of("email", email, "password", password))));
    }
    private String requestCode() throws Exception {
        request(PASSWORD, target).andExpect(status().isOk());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mail).sendEmailBindingCode(eq(target), code.capture()); return code.getValue();
    }
    private org.springframework.test.web.servlet.ResultActions confirm(String header, String email, String code) throws Exception {
        return mvc.perform(post(CONFIRM).header("Authorization", header).contentType("application/json")
                .content(body(Map.of("email", email, "code", code))));
    }
    @Test void registrationNeedsOnlyEmailAndPasswordAndDefaultsMayRepeat() throws Exception {
        for (int i = 0; i < 2; i++) {
            String email = address();
            mvc.perform(post("/api/auth/register").contentType("application/json")
                    .content(body(Map.of("email", email, "password", PASSWORD))))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("小奶娃"));
            User created = users.findByEmail(email).orElseThrow();
            assertEquals("小奶娃", created.getDisplayName()); assertEquals("default", created.getAvatarKey());
            assertFalse(created.isEmailVerified()); assertTrue(passwords.matches(PASSWORD, created.getPasswordHash()));
            ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
            verify(mail).sendEmailVerificationCode(eq(email), code.capture());
            mvc.perform(post("/api/auth/email-verification/confirm").contentType("application/json")
                    .content(body(Map.of("email", email, "code", code.getValue())))).andExpect(status().isOk());
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(body(Map.of("email", email, "password", PASSWORD)))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("小奶娃"))
                    .andExpect(jsonPath("$.avatarKey").value("default"));
        }
    }
    @Test void profileRequiresAuthenticationAndKeepsLegacyName() throws Exception {
        mvc.perform(get(PROFILE)).andExpect(status().isUnauthorized());
        mvc.perform(get(PROFILE).header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(user.getUsername()))
                .andExpect(jsonPath("$.avatarKey").value("default"))
                .andExpect(jsonPath("$.linkedMethods[0]").value("EMAIL"));
        mvc.perform(get(PROFILE).header("Authorization", auth)).andExpect(status().isOk());
        assertEquals(1, identities.findAllByUser(user).size());
    }
    @Test void profileChangeKeepsStableIdAndToken() throws Exception {
        Long id = user.getId(); String handle = user.getUsername();
        mvc.perform(put(PROFILE).header("Authorization", auth).contentType("application/json")
                .content(body(Map.of("displayName", " 小奶娃 ", "avatarKey", "thinking"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("小奶娃"));
        mvc.perform(get(PROFILE).header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id)).andExpect(jsonPath("$.avatarKey").value("thinking"));
        assertEquals(handle, users.findById(id).orElseThrow().getUsername());
    }
    @Test void invalidNamesAndUnknownAvatarAreRejected() throws Exception {
        for (String name : List.of(" ", "a\nb", "😀".repeat(25))) {
            mvc.perform(put(PROFILE).header("Authorization", auth).contentType("application/json")
                    .content(body(Map.of("displayName", name, "avatarKey", "default"))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put(PROFILE).header("Authorization", auth).contentType("application/json")
                .content(body(Map.of("displayName", "😀".repeat(24), "avatarKey", "strong"))))
                .andExpect(status().isOk());
        mvc.perform(put(PROFILE).header("Authorization", auth).contentType("application/json")
                .content(body(Map.of("displayName", "小奶娃", "avatarKey", "file:///untrusted"))))
                .andExpect(status().isBadRequest());
    }
    @Test void bindingChecksPasswordOwnershipAndCooldown() throws Exception {
        request("wrong-password", target).andExpect(status().isBadRequest());
        request(PASSWORD, user.getEmail()).andExpect(status().isBadRequest());
        User occupied = User.createDefault(address(), passwords.encode(PASSWORD)); users.saveAndFlush(occupied);
        request(PASSWORD, occupied.getEmail()).andExpect(status().isConflict());
        verifyNoInteractions(mail);
        requestCode(); request(PASSWORD, address()).andExpect(status().isTooManyRequests());
        verify(mail, times(1)).sendEmailBindingCode(anyString(), anyString());
    }
    @Test void emailChangeKeepsBackupAndExistingTokenAndClearsOldResetCode() throws Exception {
        String oldEmail = user.getEmail(), device = UUID.randomUUID().toString(); Long id = user.getId();
        String payload = "{\"schemaVersion\":1,\"limits\":[],\"tasks\":[],\"focus\":{}}";
        mvc.perform(put("/api/backups/" + device).header("Authorization", auth).contentType("application/json")
                .content(body(Map.of("expectedRevision", 0, "deviceName", "account-test", "payload", payload))))
                .andExpect(status().isOk());
        resetCodes.saveAndFlush(new PasswordResetCode(user, passwords.encode("123456"), LocalDateTime.now().plusMinutes(10)));
        String code = requestCode();
        confirm(auth, target, code).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(id))
                .andExpect(jsonPath("$.email").value(target));
        confirm(auth, target, code).andExpect(status().isBadRequest());
        assertTrue(resetCodes.findByUser(user).isEmpty()); assertTrue(users.findByEmail(oldEmail).isEmpty());
        mvc.perform(get("/api/backups/" + device).header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(payload));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(body(Map.of("email", oldEmail, "password", PASSWORD)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(body(Map.of("email", target, "password", PASSWORD)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id));
        assertEquals(target, identities.findAllByUser(user).getFirst().getSubject());
    }
    @Test void fiveWrongCodesExhaustChallengeEvenWithHttpErrors() throws Exception {
        String real = requestCode(), wrong = real.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) confirm(auth, target, wrong).andExpect(status().isBadRequest());
        entityManager.flush(); entityManager.clear();
        assertEquals(5, challenges.findByUser(user).orElseThrow().getAttempts());
        // Join the test transaction to check that HTTP errors did not mark the real transaction for rollback.
        var probe = transactions.getTransaction(new org.springframework.transaction.support.DefaultTransactionDefinition());
        assertFalse(probe.isRollbackOnly(), "Wrong-code attempts must survive the request transaction");
        transactions.commit(probe); // Participating scope only; the outer test transaction still rolls back.
        confirm(auth, target, real).andExpect(status().isBadRequest());
        assertNotEquals(target, users.findById(user.getId()).orElseThrow().getEmail());
    }
    @Test void otherAccountCannotConsumeOwnersChallenge() throws Exception {
        String code = requestCode(); User other = User.createDefault(address(), passwords.encode(PASSWORD));
        other.verifyEmail(); users.saveAndFlush(other);
        confirm("Bearer " + jwt.generateToken(other), target, code).andExpect(status().isBadRequest());
        assertEquals(0, challenges.findByUser(user).orElseThrow().getAttempts());
        confirm(auth, target, code).andExpect(status().isOk());
    }
    @Test void expiryAndChangedPasswordInvalidateChallenge() throws Exception {
        String code = requestCode(); EmailBindingChallenge challenge = challenges.findByUser(user).orElseThrow();
        ReflectionTestUtils.setField(challenge, "expiresAt", Instant.now().minusSeconds(1)); challenges.saveAndFlush(challenge);
        confirm(auth, target, code).andExpect(status().isBadRequest());
        ReflectionTestUtils.setField(challenge, "expiresAt", Instant.now().plusSeconds(600)); challenges.saveAndFlush(challenge);
        user.updatePasswordHash(passwords.encode("a-new-password")); users.saveAndFlush(user);
        confirm(auth, target, code).andExpect(status().isBadRequest());
    }
    @Test void emailClaimedAfterSendingCodeIsNotStolen() throws Exception {
        String code = requestCode(); User occupied = User.createDefault(target, passwords.encode(PASSWORD));
        users.saveAndFlush(occupied);
        confirm(auth, target, code).andExpect(status().isConflict());
        assertNotEquals(target, user.getEmail()); assertEquals(occupied.getId(), users.findByEmail(target).orElseThrow().getId());
    }
}
