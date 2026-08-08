package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.PasswordResetCode;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.PasswordResetCodeRepository;
import com.luoyilin.focusguardserver.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final int CODE_BOUND = 1_000_000;
    // 验证码取值范围是 000000 到 999999

    private static final int CODE_EXPIRATION_MINUTES = 10;
    // 验证码生成 10 分钟后失效

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    // 同一账号在 60 秒内只能发送一次密码重置验证码

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    // SecureRandom 比普通 Random 更适合生成安全验证码

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetCodeRepository passwordResetCodeRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    // Spring 自动注入用户数据库、验证码数据库、密码处理器和邮件服务

    @Transactional
    public void requestResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        // 去掉邮箱两端空格并转换为小写

        Optional<User> optionalUser =
                userRepository.findByEmail(normalizedEmail);
        // 根据邮箱查询需要找回密码的用户

        if (optionalUser.isEmpty()) {
            return;
        }
        // 邮箱不存在时不生成验证码，之后由 Controller 返回统一提示

        User user = optionalUser.get();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownStart = now.minusSeconds(
                RESEND_COOLDOWN_SECONDS
        );
        // 计算本次请求允许发送验证码的最早时间

        boolean requestedRecently =
                passwordResetCodeRepository
                        .existsByUserAndCreatedAtAfter(
                                user,
                                cooldownStart
                        );
        // 查询该用户最近 60 秒内是否已经生成过验证码

        if (requestedRecently) {
            return;
        }
        // 冷却期内直接结束，不生成新验证码，也不再次发送邮件

        String plainCode = generateSixDigitCode();
        // 生成只用于发送给用户的六位明文验证码

        String codeHash = passwordEncoder.encode(plainCode);
        // 使用 BCrypt 生成哈希，数据库不保存明文验证码

        LocalDateTime expiresAt = now
                .plusMinutes(CODE_EXPIRATION_MINUTES);
        // 计算验证码的失效时间

        passwordResetCodeRepository.deleteByUser(user);
        passwordResetCodeRepository.flush();
        // 删除并立即提交旧记录，确保一个用户只保留一个验证码

        PasswordResetCode resetCode = new PasswordResetCode(
                user,
                codeHash,
                expiresAt
        );
        passwordResetCodeRepository.saveAndFlush(resetCode);
        // 把用户、验证码哈希和失效时间写入数据库

        emailService.sendPasswordResetCode(user.getEmail(), plainCode);
        // 明文验证码只交给邮件服务发送，不会返回给客户端或保存到数据库
    }

    @Transactional
    public void resetPassword(
            String email,
            String plainCode,
            String newPassword
    ) {
        String normalizedEmail = normalizeEmail(email);
        // 使用与注册、登录相同的邮箱标准化规则

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(this::invalidResetCodeException);
        // 邮箱不存在时返回统一错误，不向外暴露账号是否存在

        PasswordResetCode resetCode =
                passwordResetCodeRepository.findByUser(user)
                        .orElseThrow(this::invalidResetCodeException);
        // 查询该用户当前保存的验证码记录

        boolean codeMatches = passwordEncoder.matches(
                plainCode,
                resetCode.getCodeHash()
        );
        // 使用 BCrypt 比对明文验证码和数据库中的验证码哈希

        if (resetCode.isUsed()
                || resetCode.isExpired()
                || !codeMatches) {
            throw invalidResetCodeException();
        }
        // 已使用、已过期或内容错误的验证码都不能继续使用

        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.updatePasswordHash(newPasswordHash);
        // 使用 BCrypt 处理新密码，并替换用户原来的密码哈希

        resetCode.markAsUsed();
        // 标记验证码已经使用，防止第二次重置密码

        userRepository.save(user);
        passwordResetCodeRepository.save(resetCode);
        // 在同一个事务中保存密码和验证码状态，保证两项一起成功
    }

    private String generateSixDigitCode() {
        int number = secureRandom.nextInt(CODE_BOUND);
        return String.format(Locale.ROOT, "%06d", number);
    }
    // 使用前导零把随机数字固定格式化为六位字符串

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
    // 邮箱统一使用小写保存和查询，避免大小写导致匹配失败

    private ResponseStatusException invalidResetCodeException() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "验证码无效或已过期"
        );
    }
    // 对邮箱不存在、验证码错误和验证码过期返回相同提示
}
