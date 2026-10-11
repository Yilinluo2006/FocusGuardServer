package com.luoyilin.focusguardserver.service;
import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.dto.auth.LoginResponse;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
@Service
public class PhoneAccountService {
    private final UserRepository users;
    private final PhoneChallengeRepository challenges;
    private final SmsVerificationGateway gateway;
    private final SmsRateLimiter rate;
    private final PhoneProofService proof;
    private final AccountService accounts;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    public PhoneAccountService(UserRepository users,PhoneChallengeRepository challenges,SmsVerificationGateway gateway,SmsRateLimiter rate,
            PhoneProofService proof,AccountService accounts,PasswordEncoder passwords,JwtService jwt) {
        this.users=users;this.challenges=challenges;this.gateway=gateway;this.rate=rate;this.proof=proof;this.accounts=accounts;this.passwords=passwords;this.jwt=jwt;
    }
    private User lock(User user) {
        if(user==null) throw error(HttpStatus.UNAUTHORIZED,"请先登录");
        return users.lockById(user.getId()).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"账号不存在"));
    }
    private PhoneCodeResponse send(String phone,PhoneChallenge.Purpose purpose,User owner,String ip,String template) {
        rate.reserve(phone,ip);
        PhoneChallenge c=new PhoneChallenge(phone,purpose,owner);
        gateway.send(phone,c.getScheme(),template);
        for(PhoneChallenge old:challenges.findByPhoneAndPurposeAndOwnerIdAndUsedFalse(phone,purpose,owner==null?null:owner.getId())) old.consume();
        challenges.saveAndFlush(c);
        return new PhoneCodeResponse(c.getId(),60,300,phone.substring(0,3)+"****"+phone.substring(7));
    }
    @Transactional public PhoneCodeResponse requestLogin(PhoneCodeRequest r,String ip) { return send(r.phone(),PhoneChallenge.Purpose.LOGIN,null,ip,"100001"); }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public LoginResponse login(PhoneCodeConfirm r) {
        PhoneChallenge c=proof.verify(r.challengeId(),PhoneChallenge.Purpose.LOGIN,null,r.code());
        User user=users.findByPhone(c.getPhone()).orElseGet(()->users.saveAndFlush(User.createPhone(c.getPhone())));
        if(!user.isPhoneVerified()) throw error(HttpStatus.FORBIDDEN,"手机号尚未验证");
        accounts.bindVerifiedIdentity(user,AccountIdentity.Provider.PHONE,"local",c.getPhone());
        return new LoginResponse(jwt.generateToken(user),user);
    }
    @Transactional public PhoneCodeResponse requestProof(User principal,String ip) {
        User user=lock(principal);
        if(!user.isPhoneVerified() || user.getPhone()==null) throw error(HttpStatus.BAD_REQUEST,"请先绑定手机号");
        return send(user.getPhone(),PhoneChallenge.Purpose.PROOF,user,ip,"100005");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public PhoneCodeResponse requestBinding(User principal,PhoneBindingRequest r,String ip) {
        User user=lock(principal);
        if(r.phone().equals(user.getPhone())) throw error(HttpStatus.BAD_REQUEST,"该手机号已绑定当前账号");
        if(users.existsByPhone(r.phone())) throw error(HttpStatus.CONFLICT,"该手机号已属于其他账号，请使用该账号登录");
        // First binding uses the logged-in account and proof of the new number.
        // Replacement also verifies an existing account credential.
        if(user.getPhone()!=null) {
            if(user.getPasswordHash()!=null) {
                if(r.password()==null || !passwords.matches(r.password(),user.getPasswordHash())) throw error(HttpStatus.BAD_REQUEST,"更换手机号需验证当前账号密码");
            } else proof.current(user,r.proofChallengeId(),r.proofCode());
        }
        return send(r.phone(),PhoneChallenge.Purpose.BIND,user,ip,user.getPhone()==null?"100004":"100002");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public ProfileResponse confirmBinding(User principal,PhoneCodeConfirm r) {
        User user=lock(principal);
        PhoneChallenge c=proof.verify(r.challengeId(),PhoneChallenge.Purpose.BIND,user,r.code());
        if(users.existsByPhone(c.getPhone())) throw error(HttpStatus.CONFLICT,"该手机号已属于其他账号");
        accounts.bindVerifiedIdentity(user,AccountIdentity.Provider.PHONE,"local",c.getPhone());
        user.bindPhone(c.getPhone());users.saveAndFlush(user);return accounts.response(user);
    }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
