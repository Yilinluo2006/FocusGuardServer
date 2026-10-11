package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class AccountService {
    private final UserRepository users;
    private final AccountIdentityRepository identities;
    private final EmailBindingChallengeRepository challenges;
    private final PasswordResetCodeRepository resetCodes;
    private final EmailVerificationCodeRepository registrationCodes;
    private final PasswordEncoder passwords;
    private final EmailService mail;
    private final PhoneProofService phoneProof;
    private final SecureRandom random = new SecureRandom();
    public AccountService(UserRepository users, AccountIdentityRepository identities, EmailBindingChallengeRepository challenges,
                          PasswordResetCodeRepository resetCodes, EmailVerificationCodeRepository registrationCodes,
                          PasswordEncoder passwords, EmailService mail, PhoneProofService phoneProof) {
        this.users = users; this.identities = identities; this.challenges = challenges; this.resetCodes = resetCodes;
        this.registrationCodes = registrationCodes; this.passwords = passwords; this.mail = mail; this.phoneProof=phoneProof;
    }
    private User lock(User principal) {
        if (principal == null || principal.getId() == null) throw error(HttpStatus.UNAUTHORIZED, "请先登录");
        return users.lockById(principal.getId()).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "账号不存在"));
    }
    private void ensureEmail(User user) {
        if (!user.hasVerifiedLogin()) throw error(HttpStatus.FORBIDDEN, "请先验证登录方式");
        if (user.getEmail() != null && user.isEmailVerified()) bindVerifiedIdentity(user, AccountIdentity.Provider.EMAIL, "local", user.getEmail());
        if (user.getPhone() != null && user.isPhoneVerified()) bindVerifiedIdentity(user, AccountIdentity.Provider.PHONE, "local", user.getPhone());
    }
    // Internal hook for future providers: call only after server-side OTP/OAuth proof verification.
    // There is deliberately no public endpoint accepting a raw phone number/openid as binding proof.
    void bindVerifiedIdentity(User user, AccountIdentity.Provider provider, String issuer, String subject) {
        AccountIdentity occupied = identities.findByProviderAndIssuerAndSubject(provider, issuer, subject).orElse(null);
        if (occupied != null && !occupied.getUser().getId().equals(user.getId()))
            throw error(HttpStatus.CONFLICT, "该登录方式已绑定其他账号");
        AccountIdentity current = identities.findByUserAndProviderAndIssuer(user, provider, issuer).orElse(null);
        if (current == null) identities.saveAndFlush(new AccountIdentity(user, provider, issuer, subject));
        else if (!current.getSubject().equals(subject)) { current.replaceVerifiedSubject(subject); identities.saveAndFlush(current); }
    }
    @Transactional public ProfileResponse profile(User principal) {
        User user = lock(principal); ensureEmail(user); return response(user);
    }
    @Transactional public ProfileResponse update(User principal, ProfileRequest request) {
        String name = request.displayName().strip();
        int length = name.codePointCount(0, name.length());
        if (length < 1 || length > 24 || name.codePoints().anyMatch(Character::isISOControl)
                || name.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)))
            throw error(HttpStatus.BAD_REQUEST, "昵称需为 1–24 个字符，不能包含换行");
        if (!Set.of("default", "sad", "strong", "thinking", "thumbs_up").contains(request.avatarKey()))
            throw error(HttpStatus.BAD_REQUEST, "请选择提供的头像");
        User user = lock(principal); ensureEmail(user); user.updateProfile(name, request.avatarKey());
        users.saveAndFlush(user); return response(user);
    }
    @Transactional(noRollbackFor=ResponseStatusException.class) public void requestEmail(User principal, EmailBindingRequest request) {
        User user = lock(principal); ensureEmail(user);
        if (user.getPasswordHash()!=null && !passwords.matches(request.password(), user.getPasswordHash())) throw error(HttpStatus.BAD_REQUEST, "当前密码不正确");
        String email = normalize(request.email());
        if (email.equals(user.getEmail())) throw error(HttpStatus.BAD_REQUEST, "这个邮箱已绑定当前账号");
        if (users.existsByEmail(email) || identities.findByProviderAndIssuerAndSubject(AccountIdentity.Provider.EMAIL, "local", email).isPresent())
            throw error(HttpStatus.CONFLICT, "该邮箱已绑定其他账号，请使用该账号登录");
        EmailBindingChallenge challenge = challenges.findByUser(user).orElseGet(() -> new EmailBindingChallenge(user));
        if (challenge.getRequestedAt() != null && challenge.getRequestedAt().plusSeconds(60).isAfter(Instant.now()))
            throw error(HttpStatus.TOO_MANY_REQUESTS, "请 60 秒后重新发送");
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        if(user.getPasswordHash()==null) phoneProof.current(user,request.phoneChallengeId(),request.phoneCode());
        challenge.renew(email, passwords.encode(code), user.getPasswordHash());
        if(user.getPasswordHash()==null) challenge.newPassword(passwords.encode(request.password()),user.getPhone());
        challenges.saveAndFlush(challenge);
        mail.sendEmailBindingCode(email, code);
    }
    // Wrong-code attempts must commit instead of being rolled back with the HTTP error.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public ProfileResponse confirmEmail(User principal, EmailBindingConfirm request) {
        User user = lock(principal);
        EmailBindingChallenge challenge = challenges.findByUser(user).orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "请先发送换绑验证码"));
        if (!challenge.available() || !challenge.getCredentialHash().equals(user.getPasswordHash()==null?"":user.getPasswordHash())
                || (challenge.getNewPasswordHash()!=null && !Objects.equals(challenge.getOriginalPhone(),user.getPhone())))
            throw error(HttpStatus.BAD_REQUEST, "验证码无效或已过期，请重新发送");
        challenge.attempt(); challenges.saveAndFlush(challenge);
        String email = normalize(request.email());
        if (!email.equals(challenge.getEmail()) || !passwords.matches(request.code(), challenge.getCodeHash()))
            throw error(HttpStatus.BAD_REQUEST, "验证码不正确");
        if (users.existsByEmail(email)) throw error(HttpStatus.CONFLICT, "该邮箱已绑定其他账号");
        bindVerifiedIdentity(user, AccountIdentity.Provider.EMAIL, "local", email);
        user.bindEmail(email); if(challenge.getNewPasswordHash()!=null) user.updatePasswordHash(challenge.getNewPasswordHash());
        users.saveAndFlush(user); challenge.consume(); challenges.saveAndFlush(challenge);
        // Codes delivered to the previous email must not reset credentials after a binding change.
        resetCodes.deleteByUser(user); registrationCodes.deleteByUser(user);
        return response(user);
    }
    ProfileResponse response(User user) {
        List<String> linked = identities.findAllByUser(user).stream().map(i -> i.getProvider().name()).distinct().sorted().toList();
        return new ProfileResponse(user.getId(), user.getDisplayName(), user.getAvatarKey(), user.getEmail(), user.isEmailVerified(), linked,
                user.getPhone(), user.isPhoneVerified(), user.getPasswordHash() != null);
    }
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
