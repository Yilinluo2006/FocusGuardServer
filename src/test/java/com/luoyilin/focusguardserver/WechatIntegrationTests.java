package com.luoyilin.focusguardserver;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import com.luoyilin.focusguardserver.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class WechatIntegrationTests {
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired AccountIdentityRepository identities;
    @Autowired WechatChallengeRepository challenges;@Autowired JwtService jwt;
    @MockitoBean WechatGateway gateway;
    private final ObjectMapper json=new ObjectMapper();private final Set<Long> createdUsers=new HashSet<>();private final Set<String> states=new HashSet<>();
    private String open,union;
    @BeforeEach void setup(){open="wx_test_"+UUID.randomUUID().toString().replace("-","");union="union_"+UUID.randomUUID().toString().replace("-","");
        when(gateway.available()).thenReturn(true);when(gateway.appId()).thenReturn("wxbc866bf990e09da5");when(gateway.exchange(anyString())).thenReturn(new WechatGateway.Verified(open,union,"微信昵称"));}
    @AfterEach void cleanup(){
        for(String state:states)challenges.deleteById(hash(state));
        identities.findByProviderAndIssuerAndSubject(AccountIdentity.Provider.WECHAT,"wxbc866bf990e09da5",open).ifPresent(i->createdUsers.add(i.getUser().getId()));
        for(Long id:createdUsers){User user=users.findById(id).orElse(null);if(user!=null){identities.deleteAll(identities.findAllByUser(user));users.deleteById(id);}}
    }
    private String hash(String state){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(state.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String start(boolean binding,String auth)throws Exception{
        var request=post(binding?"/api/account/bindings/wechat/start":"/api/auth/wechat/start");if(auth!=null)request.header("Authorization",auth);
        String raw=mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();String state=json.readTree(raw).get("state").asText();states.add(state);return state;
    }
    private org.springframework.test.web.servlet.ResultActions finish(boolean binding,String auth,String code,String state)throws Exception{
        var request=post(binding?"/api/account/bindings/wechat/confirm":"/api/auth/wechat/login").contentType("application/json").content(json.writeValueAsString(Map.of("code",code,"state",state)));
        if(auth!=null)request.header("Authorization",auth);return mvc.perform(request);
    }
    private User existing(){User u=User.createDefault(UUID.randomUUID()+"@example.invalid","test-hash");u.verifyEmail();u=users.saveAndFlush(u);createdUsers.add(u.getId());return u;}
    @Test void firstWechatLoginCreatesAVerifiedAccountAndIssuesOurOwnJwt()throws Exception {
        String raw=finish(false,null,"fresh-code",start(false,null)).andExpect(status().isOk()).andExpect(jsonPath("$.wechatVerified").value(true))
                .andExpect(jsonPath("$.hasPassword").value(false)).andReturn().getResponse().getContentAsString();
        long id=json.readTree(raw).get("userId").asLong();createdUsers.add(id);String token=json.readTree(raw).get("token").asText();
        assertEquals(id,jwt.extractUserId(token));mvc.perform(get("/api/account/profile").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.linkedMethods").isArray()).andExpect(jsonPath("$.email").isEmpty());
        String second=finish(false,null,"another-code",start(false,null)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(id,json.readTree(second).get("userId").asLong());
    }
    @Test void bindingPreservesExistingAccountIdAndSubsequentWechatLoginUsesIt()throws Exception {
        User user=existing();String auth="Bearer "+jwt.generateToken(user),name=user.getDisplayName(),email=user.getEmail();
        finish(true,auth,"bind-code",start(true,auth)).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(user.getId()));
        String raw=finish(false,null,"login-code",start(false,null)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(user.getId().longValue(),json.readTree(raw).get("userId").asLong());User after=users.findById(user.getId()).orElseThrow();
        assertEquals(name,after.getDisplayName());assertEquals(email,after.getEmail());
    }
    @Test void invalidCodeIsRejectedAndTheSameChallengeCannotBeReplayed()throws Exception {
        when(gateway.exchange(anyString())).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"微信授权无效"));String state=start(false,null);
        finish(false,null,"bad-code",state).andExpect(status().isBadRequest());finish(false,null,"another-code",state).andExpect(status().isBadRequest());verify(gateway,times(1)).exchange(anyString());
        assertTrue(identities.findByProviderAndIssuerAndSubject(AccountIdentity.Provider.WECHAT,"wxbc866bf990e09da5",open).isEmpty());
    }
    @Test void foreignStateWrongPurposeAndWrongOwnerCannotBind()throws Exception {
        User one=existing(),two=existing();String first="Bearer "+jwt.generateToken(one),second="Bearer "+jwt.generateToken(two);
        finish(true,first,"code",start(false,null)).andExpect(status().isBadRequest());
        String binding=start(true,first);finish(true,second,"code",binding).andExpect(status().isBadRequest());
        finish(true,first,"code",binding).andExpect(status().isOk());
        finish(false,null,"code","x".repeat(43)).andExpect(status().isBadRequest());
    }
    @Test void cannotBindAnotherAccountsWechatOrReplaceTheExistingWechat()throws Exception {
        User one=existing(),two=existing();String first="Bearer "+jwt.generateToken(one),second="Bearer "+jwt.generateToken(two);
        finish(true,first,"code",start(true,first)).andExpect(status().isOk());
        finish(true,second,"code",start(true,second)).andExpect(status().isConflict());
        when(gateway.exchange(anyString())).thenReturn(new WechatGateway.Verified("other_"+open,"other_"+union,"其他微信"));
        finish(true,first,"code",start(true,first)).andExpect(status().isConflict());
        assertEquals(open,identities.findByUserAndProviderAndIssuer(one,AccountIdentity.Provider.WECHAT,"wxbc866bf990e09da5").orElseThrow().getSubject());
    }
    @Test void expiresAndMissingConfigurationFailWithoutCallingWechat()throws Exception {
        String state=start(false,null);challenges.deleteById(hash(state));challenges.saveAndFlush(new WechatChallenge(hash(state),WechatChallenge.Purpose.LOGIN,null,Instant.now().minusSeconds(1)));
        finish(false,null,"code",state).andExpect(status().isBadRequest());verify(gateway,never()).exchange(anyString());
        when(gateway.available()).thenReturn(false);mvc.perform(post("/api/auth/wechat/start")).andExpect(status().isServiceUnavailable());
    }
    @Test void loginProofCannotBeSubmittedWithARawOpenIdInsteadOfAWechatCode()throws Exception {
        mvc.perform(post("/api/auth/wechat/login").contentType("application/json").content("{\"openid\":\"attacker\",\"state\":\""+"x".repeat(43)+"\"}"))
                .andExpect(status().isBadRequest());verify(gateway,never()).exchange(anyString());
    }
}
