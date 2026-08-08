package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.PasswordResetCode;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.PasswordResetCodeRepository;
import com.luoyilin.focusguardserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    // 创建假的用户 Repository，不连接真实 MySQL

    @Mock
    private PasswordResetCodeRepository passwordResetCodeRepository;
    // 创建假的验证码 Repository，不修改真实验证码记录

    @Mock
    private PasswordEncoder passwordEncoder;
    // 创建假的密码编码器，测试时不执行真实 BCrypt 计算

    @Mock
    private EmailService emailService;
    // 创建假的邮件服务，测试时不会真正发送邮件

    @InjectMocks
    private PasswordResetService passwordResetService;
    // 创建真实 Service，并自动注入上面的四个假对象

    @Test
    void requestResetCodeDoesNotSendAgainDuringCooldown() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        // 模拟数据库中存在该邮箱对应的账号

        when(
                passwordResetCodeRepository
                        .existsByUserAndCreatedAtAfter(
                                eq(user),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(true);
        // 模拟该账号在最近 60 秒内已经申请过验证码

        passwordResetService.requestResetCode(
                " Test@Example.com "
        );

        verify(passwordResetCodeRepository, never())
                .deleteByUser(any(User.class));
        verify(passwordResetCodeRepository, never())
                .saveAndFlush(any(PasswordResetCode.class));
        verify(emailService, never())
                .sendPasswordResetCode(any(), any());
        // 验证冷却期内没有删除记录、生成新记录或再次发送邮件
    }

    @Test
    void requestResetCodeSendsWhenCooldownHasEnded() {
        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(
                passwordResetCodeRepository
                        .existsByUserAndCreatedAtAfter(
                                eq(user),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(false);
        // 模拟最近 60 秒内没有申请过验证码

        when(passwordEncoder.encode(any()))
                .thenReturn("hashed-code");

        passwordResetService.requestResetCode(
                "test@example.com"
        );

        verify(passwordResetCodeRepository)
                .deleteByUser(user);
        verify(passwordResetCodeRepository)
                .saveAndFlush(any(PasswordResetCode.class));
        verify(emailService)
                .sendPasswordResetCode(
                        eq("test@example.com"),
                        any()
                );
        // 验证冷却期结束后可以正常保存验证码并发送邮件
    }
}
