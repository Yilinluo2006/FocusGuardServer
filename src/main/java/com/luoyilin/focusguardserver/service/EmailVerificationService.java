package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.EmailVerificationCode;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.EmailVerificationCodeRepository;
import com.luoyilin.focusguardserver.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;
    // 验证码取值范围是 000000 到 999999

    private static final int CODE_EXPIRATION_MINUTES = 10;
    // 注册邮箱验证码生成十分钟后失效

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    // 同一账号在六十秒内只发送一次注册邮箱验证码

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    // SecureRandom 用于生成不容易被猜中的验证码

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationCodeRepository codeRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    // Spring 自动注入用户数据库、验证码数据库、密码处理器和邮件服务

    @Transactional
    public void requestVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        // 去掉邮箱两端空格并统一转换为小写

        Optional<User> optionalUser =
                userRepository.findByEmail(normalizedEmail);
        // 根据邮箱查找已经注册的用户

        if (optionalUser.isEmpty()) {
            return;
        }
        // 邮箱不存在时静默结束，避免接口泄露某个邮箱是否注册

        User user = optionalUser.get();

        if (user.isEmailVerified()) {
            return;
        }
        // 已经完成验证的账号不需要再次发送验证码

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownStart = now.minusSeconds(
                RESEND_COOLDOWN_SECONDS
        );
        // 计算本次发送所允许的最早验证码创建时间

        boolean requestedRecently =
                codeRepository.existsByUserAndCreatedAtAfter(
                        user,
                        cooldownStart
                );
        // 检查该用户最近六十秒内是否已经申请过验证码

        if (requestedRecently) {
            return;
        }
        // 冷却期内不生成新验证码，也不重复发送邮件

        String plainCode = generateSixDigitCode();
        // 生成只会出现在邮件中的六位明文验证码

        String codeHash = passwordEncoder.encode(plainCode);
        // 使用 BCrypt 处理验证码，数据库不保存明文

        LocalDateTime expiresAt = now.plusMinutes(
                CODE_EXPIRATION_MINUTES
        );
        // 计算验证码十分钟后的失效时间

        codeRepository.deleteByUser(user);
        codeRepository.flush();
        // 删除旧验证码并立即提交，保证每个用户只保留一条记录

        EmailVerificationCode verificationCode =
                new EmailVerificationCode(
                        user,
                        codeHash,
                        expiresAt
                );

        codeRepository.saveAndFlush(verificationCode);
        // 把验证码哈希、所属用户和失效时间保存到 MySQL

        emailService.sendEmailVerificationCode(
                user.getEmail(),
                plainCode
        );
        // 把明文验证码发送到用户注册邮箱
    }

    @Transactional
    public void verifyEmail(String email, String plainCode) {
        String normalizedEmail = normalizeEmail(email);
        // 使用与注册、登录相同的邮箱标准化规则

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(this::invalidCodeException);
        // 邮箱不存在时返回统一的验证码错误

        if (user.isEmailVerified()) {
            return;
        }
        // 已验证账号再次提交时直接视为成功，使接口具有幂等性

        EmailVerificationCode verificationCode =
                codeRepository.findByUser(user)
                        .orElseThrow(this::invalidCodeException);
        // 查询该用户当前保存的注册邮箱验证码

        boolean codeMatches = passwordEncoder.matches(
                plainCode,
                verificationCode.getCodeHash()
        );
        // 使用 BCrypt 比对用户输入的验证码和数据库哈希

        if (verificationCode.isUsed()
                || verificationCode.isExpired()
                || !codeMatches) {
            throw invalidCodeException();
        }
        // 已使用、已过期或内容错误的验证码都不能通过验证

        user.verifyEmail();
        verificationCode.markAsUsed();
        // 更新用户验证状态，同时让当前验证码失效

        userRepository.save(user);
        codeRepository.save(verificationCode);
        // 在同一个事务中保存两项状态，保证它们一起成功
    }

    private String generateSixDigitCode() {
        int number = secureRandom.nextInt(CODE_BOUND);
        return String.format(Locale.ROOT, "%06d", number);
    }
    // 使用前导零把随机数字固定格式化成六位字符串

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
    // 去掉邮箱两端空格并统一转换为小写

    private ResponseStatusException invalidCodeException() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "验证码无效或已过期"
        );
    }
    // 对邮箱不存在、验证码错误和验证码过期返回同一种提示
}
