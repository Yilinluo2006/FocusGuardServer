package com.luoyilin.focusguardserver.service;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.PhoneChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PhoneProofService {
    private final PhoneChallengeRepository challenges;
    private final SmsVerificationGateway gateway;
    public PhoneProofService(PhoneChallengeRepository challenges, SmsVerificationGateway gateway) { this.challenges=challenges; this.gateway=gateway; }
    public PhoneChallenge verify(String id, PhoneChallenge.Purpose purpose, User owner, String code) {
        if (id == null || code == null) throw invalid();
        PhoneChallenge c=challenges.lockById(id).orElseThrow(this::invalid);
        if (!c.available() || c.getPurpose()!=purpose || (owner==null ? c.getOwnerId()!=null : !c.matchesOwner(owner))) throw invalid();
        c.attempt(); challenges.saveAndFlush(c);
        if (!gateway.verify(c.getPhone(), c.getScheme(), code)) throw invalid();
        c.consume(); challenges.saveAndFlush(c); return c;
    }
    public void current(User owner,String id,String code) {
        PhoneChallenge c=verify(id,PhoneChallenge.Purpose.PROOF,owner,code);
        if (!owner.isPhoneVerified() || !c.getPhone().equals(owner.getPhone())) throw invalid();
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"验证码不正确、已使用或已过期，请重新获取"); }
}
