package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.EmailVerificationCode;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.EmailVerificationCodeRepository;
import com.luoyilin.focusguardserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    // 创建假的用户 Repository，测试时不连接真实 MySQL

    @Mock
    private EmailVerificationCodeRepository codeRepository;
    // 创建假的验证码 Repository，测试时不修改真实数据

    @Mock
    private PasswordEncoder passwordEncoder;
    // 创建假的 BCrypt 处理器，使测试结果稳定且执行更快

    @Mock
    private EmailService emailService;
    // 创建假的邮件服务，运行测试时不会真的发送邮件

    @InjectMocks
    private EmailVerificationService emailVerificationService;
    // 创建真实邮箱验证服务，并自动注入上面的假对象

    @Test
    void requestCodeDoesNotSendAgainDuringCooldown() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        // 模拟数据库中存在一个尚未验证邮箱的用户

        when(
                codeRepository.existsByUserAndCreatedAtAfter(
                        eq(user),
                        any(LocalDateTime.class)
                )
        ).thenReturn(true);
        // 模拟这个用户在最近六十秒内已经申请过验证码

        emailVerificationService.requestVerificationCode(
                " Test@Example.com "
        );

        verify(codeRepository, never())
                .deleteByUser(any(User.class));
        verify(codeRepository, never())
                .saveAndFlush(any(EmailVerificationCode.class));
        verify(emailService, never())
                .sendEmailVerificationCode(any(), any());
        // 验证冷却期内不会删除、保存或再次发送验证码
    }

    @Test
    void requestCodeSendsWhenCooldownHasEnded() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(
                codeRepository.existsByUserAndCreatedAtAfter(
                        eq(user),
                        any(LocalDateTime.class)
                )
        ).thenReturn(false);
        // 模拟最近六十秒内没有申请过验证码

        when(passwordEncoder.encode(any()))
                .thenReturn("hashed-code");

        emailVerificationService.requestVerificationCode(
                "test@example.com"
        );

        verify(codeRepository).deleteByUser(user);
        verify(codeRepository)
                .saveAndFlush(any(EmailVerificationCode.class));
        verify(emailService).sendEmailVerificationCode(
                eq("test@example.com"),
                any()
        );
        // 验证冷却结束后会保存新验证码并调用邮件服务
    }

    @Test
    void verifyEmailMarksUserAndCodeAsCompleted() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        EmailVerificationCode verificationCode =
                new EmailVerificationCode(
                        user,
                        "hashed-code",
                        LocalDateTime.now().plusMinutes(10)
                );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(codeRepository.findByUser(user))
                .thenReturn(Optional.of(verificationCode));
        when(passwordEncoder.matches("123456", "hashed-code"))
                .thenReturn(true);
        // 模拟用户、验证码记录和正确的 BCrypt 比对结果

        emailVerificationService.verifyEmail(
                "test@example.com",
                "123456"
        );

        assertTrue(user.isEmailVerified());
        assertTrue(verificationCode.isUsed());
        verify(userRepository).save(user);
        verify(codeRepository).save(verificationCode);
        // 验证用户状态、验证码状态和两次数据库保存都已发生
    }
}
