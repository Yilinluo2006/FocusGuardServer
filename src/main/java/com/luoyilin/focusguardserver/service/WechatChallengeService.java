package com.luoyilin.focusguardserver.service;
import com.luoyilin.focusguardserver.model.WechatChallenge;
import com.luoyilin.focusguardserver.repository.WechatChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service public class WechatChallengeService {
    public record Start(String appId,String state,long serverTime,long expiresAt){}
    private final WechatChallengeRepository challenges;private final WechatGateway gateway;
    private final SecureRandom random=new SecureRandom();private final Map<String,List<Long>> attempts=new HashMap<>();
    public WechatChallengeService(WechatChallengeRepository challenges,WechatGateway gateway){this.challenges=challenges;this.gateway=gateway;}
    private synchronized void reserve(String ip){long now=System.currentTimeMillis();attempts.values().forEach(v->v.removeIf(t->t<now-60000));attempts.entrySet().removeIf(e->e.getValue().isEmpty());
        List<Long> list=attempts.computeIfAbsent(ip,k->new ArrayList<>());if(list.size()>=20)throw error(HttpStatus.TOO_MANY_REQUESTS,"微信授权请求过于频繁，请稍后重试");list.add(now);}
    @Transactional public Start start(WechatChallenge.Purpose purpose,Long owner,String ip){
        if(!gateway.available())throw error(HttpStatus.SERVICE_UNAVAILABLE,"微信登录暂不可用，请使用其他登录方式");
        reserve(ip);byte[] bytes=new byte[32];random.nextBytes(bytes);String state=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now=Instant.now(),expires=now.plusSeconds(300);challenges.deleteByExpiresAtBefore(now.minusSeconds(600));
        challenges.saveAndFlush(new WechatChallenge(hash(state),purpose,owner,expires));return new Start(gateway.appId(),state,now.toEpochMilli(),expires.toEpochMilli());
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void consume(String state,WechatChallenge.Purpose purpose,Long owner){
        WechatChallenge challenge=challenges.lockByHash(hash(state)).orElseThrow(()->error(HttpStatus.BAD_REQUEST,"微信授权请求已失效，请重新发起"));
        if(!challenge.accepts(purpose,owner,Instant.now()))throw error(HttpStatus.BAD_REQUEST,"微信授权请求已失效，请重新发起");
        challenge.consume();challenges.saveAndFlush(challenge);
    }
    private String hash(String state){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(state.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
