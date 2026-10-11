package com.luoyilin.focusguardserver;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import com.luoyilin.focusguardserver.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class PhoneIntegrationTests {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired PhoneChallengeRepository challenges;
    @Autowired JwtService jwt; @Autowired PasswordEncoder passwords;
    @MockitoBean SmsVerificationGateway gateway; @MockitoBean SmsRateLimiter rate; @MockitoBean EmailService mail;
    private final ObjectMapper json=new ObjectMapper();
    private String body(Map<String,?> data){return json.writeValueAsString(data);}
    private PhoneChallenge loginChallenge(){return challenges.saveAndFlush(new PhoneChallenge("13900001111",PhoneChallenge.Purpose.LOGIN,null));}
    private org.springframework.test.web.servlet.ResultActions confirm(String endpoint,PhoneChallenge c,String auth) throws Exception {
        var request=post(endpoint).contentType("application/json").content(body(Map.of("challengeId",c.getId(),"code","123456")));
        if(auth!=null)request.header("Authorization",auth);return mvc.perform(request);
    }
    @Test void phoneRegistrationHasDefaultsAndCanAuthenticate() throws Exception {
        PhoneChallenge c=loginChallenge();when(gateway.verify(c.getPhone(),c.getScheme(),"123456")).thenReturn(true);
        String result=confirm("/api/auth/phone/confirm",c,null).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("小奶娃"))
            .andExpect(jsonPath("$.hasPassword").value(false)).andReturn().getResponse().getContentAsString();
        String token=json.readTree(result).get("token").asText();User user=users.findByPhone(c.getPhone()).orElseThrow();assertNull(user.getEmail());assertNull(user.getPasswordHash());
        mvc.perform(get("/api/account/profile").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.linkedMethods[0]").value("PHONE"));
        mvc.perform(get("/api/backups").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        confirm("/api/auth/phone/confirm",c,null).andExpect(status().isBadRequest());verify(gateway,times(1)).verify(anyString(),anyString(),anyString());
    }
    @Test void incorrectCodesStopAfterFiveAttempts() throws Exception {
        PhoneChallenge c=loginChallenge();for(int i=0;i<6;i++)confirm("/api/auth/phone/confirm",c,null).andExpect(status().isBadRequest());
        assertEquals(5,challenges.findById(c.getId()).orElseThrow().getAttempts());verify(gateway,times(5)).verify(anyString(),anyString(),anyString());assertTrue(users.findByPhone(c.getPhone()).isEmpty());
    }
    private User emailAccount(){User u=User.createDefault(UUID.randomUUID()+"@example.invalid",passwords.encode("test-password"));u.verifyEmail();return users.saveAndFlush(u);}
    @Test void bindingAndPhoneLoginPreserveEmailAccountId() throws Exception {
        User u=emailAccount();String auth="Bearer "+jwt.generateToken(u);
        String response=mvc.perform(post("/api/account/bindings/phone/request").header("Authorization",auth).contentType("application/json")
            .content(body(Map.of("phone","13900001111")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        PhoneChallenge c=challenges.findById(json.readTree(response).get("challengeId").asText()).orElseThrow();when(gateway.verify(anyString(),anyString(),anyString())).thenReturn(true);
        confirm("/api/account/bindings/phone/confirm",c,auth).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(u.getId()));
        PhoneChallenge login=loginChallenge();confirm("/api/auth/phone/confirm",login,null).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(u.getId())).andExpect(jsonPath("$.email").value(u.getEmail()));
    }
    @Test void replacingPhoneRequiresCurrentPasswordAndKeepsAccountId() throws Exception {
        User u=emailAccount();u.bindPhone("13800001111");users.saveAndFlush(u);String auth="Bearer "+jwt.generateToken(u);
        String endpoint="/api/account/bindings/phone/request";
        mvc.perform(post(endpoint).header("Authorization",auth).contentType("application/json").content(body(Map.of("phone","13900001111")))).andExpect(status().isBadRequest());
        mvc.perform(post(endpoint).header("Authorization",auth).contentType("application/json").content(body(Map.of("phone","13900001111","password","wrong-password")))).andExpect(status().isBadRequest());
        verifyNoInteractions(gateway,rate);
        String response=mvc.perform(post(endpoint).header("Authorization",auth).contentType("application/json").content(body(Map.of("phone","13900001111","password","test-password")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        verify(gateway).send(eq("13900001111"),anyString(),eq("100002"));
        PhoneChallenge c=challenges.findById(json.readTree(response).get("challengeId").asText()).orElseThrow();when(gateway.verify(anyString(),anyString(),anyString())).thenReturn(true);
        confirm("/api/account/bindings/phone/confirm",c,auth).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(u.getId())).andExpect(jsonPath("$.phone").value("13900001111"));
    }
    @Test void phoneOnlyReplacementStillRequiresProofOfOriginalPhone() throws Exception {
        User u=users.saveAndFlush(User.createPhone("13800001111"));String auth="Bearer "+jwt.generateToken(u);
        mvc.perform(post("/api/account/bindings/phone/request").header("Authorization",auth).contentType("application/json").content(body(Map.of("phone","13900001111")))).andExpect(status().isBadRequest());verifyNoInteractions(gateway);
        PhoneChallenge original=challenges.saveAndFlush(new PhoneChallenge(u.getPhone(),PhoneChallenge.Purpose.PROOF,u));when(gateway.verify(u.getPhone(),original.getScheme(),"123456")).thenReturn(true);
        mvc.perform(post("/api/account/bindings/phone/request").header("Authorization",auth).contentType("application/json").content(body(Map.of("phone","13900001111","proofChallengeId",original.getId(),"proofCode","123456")))).andExpect(status().isOk());
        verify(gateway).send(eq("13900001111"),anyString(),eq("100002"));
    }
    @Test void firstBindingCannotOverwritePhoneAddedAfterCodeWasRequested() throws Exception {
        User u=emailAccount();String auth="Bearer "+jwt.generateToken(u);
        String response=mvc.perform(post("/api/account/bindings/phone/request").header("Authorization",auth).contentType("application/json").content(body(Map.of("phone","13900001111")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        verify(gateway).send(eq("13900001111"),anyString(),eq("100004"));
        PhoneChallenge c=challenges.findById(json.readTree(response).get("challengeId").asText()).orElseThrow();u.bindPhone("13800001111");users.saveAndFlush(u);
        confirm("/api/account/bindings/phone/confirm",c,auth).andExpect(status().isBadRequest());verify(gateway,never()).verify(anyString(),anyString(),anyString());assertEquals("13800001111",users.findById(u.getId()).orElseThrow().getPhone());
    }
    @Test void loginChallengeCannotBindAnotherAccount() throws Exception {
        User u=emailAccount();PhoneChallenge c=loginChallenge();confirm("/api/account/bindings/phone/confirm",c,"Bearer "+jwt.generateToken(u)).andExpect(status().isBadRequest());verifyNoInteractions(gateway);
    }
    @Test void changedCredentialsInvalidatePendingBinding() throws Exception {
        User u=emailAccount();PhoneChallenge c=challenges.saveAndFlush(new PhoneChallenge("13900001111",PhoneChallenge.Purpose.BIND,u));u.updatePasswordHash(passwords.encode("changed-password"));users.saveAndFlush(u);
        confirm("/api/account/bindings/phone/confirm",c,"Bearer "+jwt.generateToken(u)).andExpect(status().isBadRequest());verifyNoInteractions(gateway);
    }
    @Test void phoneOnlyAccountMustProveCurrentPhoneBeforeAddingEmail() throws Exception {
        User u=users.saveAndFlush(User.createPhone("13900001111"));String auth="Bearer "+jwt.generateToken(u);String email=UUID.randomUUID()+"@example.invalid";
        mvc.perform(post("/api/account/bindings/email/request").header("Authorization",auth).contentType("application/json").content(body(Map.of("email",email,"password","test-password")))).andExpect(status().isBadRequest());verifyNoInteractions(mail);
        PhoneChallenge c=challenges.saveAndFlush(new PhoneChallenge(u.getPhone(),PhoneChallenge.Purpose.PROOF,u));when(gateway.verify(anyString(),anyString(),anyString())).thenReturn(true);
        mvc.perform(post("/api/account/bindings/email/request").header("Authorization",auth).contentType("application/json").content(body(Map.of("email",email,"password","test-password","phoneChallengeId",c.getId(),"phoneCode","123456")))).andExpect(status().isOk());
        var captor=org.mockito.ArgumentCaptor.forClass(String.class);verify(mail).sendEmailBindingCode(eq(email),captor.capture());
        mvc.perform(post("/api/account/bindings/email/confirm").header("Authorization",auth).contentType("application/json").content(body(Map.of("email",email,"code",captor.getValue())))).andExpect(status().isOk()).andExpect(jsonPath("$.hasPassword").value(true)).andExpect(jsonPath("$.userId").value(u.getId()));
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("email",email,"password","test-password")))).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(u.getId()));
    }
}
