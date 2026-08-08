package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.dto.auth.LoginRequest;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    // 创建假的用户 Repository，不读取真实用户表

    @Mock
    private PasswordEncoder passwordEncoder;
    // 创建假的密码处理器，用于模拟密码比对成功

    @Mock
    private JwtService jwtService;
    // 创建假的 JWT 服务，测试时不会生成真实令牌

    @InjectMocks
    private AuthService authService;
    // 创建真实登录服务，并注入上面的三个假对象

    @Test
    void loginRejectsUserWhoseEmailIsNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" Test@Example.com ");
        request.setPassword("password123");
        // 创建一个格式正确的登录请求

        User user = new User(
                "testuser",
                "test@example.com",
                "hashed-password"
        );
        // 新建用户默认 emailVerified 为 false

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(
                passwordEncoder.matches(
                        "password123",
                        "hashed-password"
                )
        ).thenReturn(true);
        // 模拟数据库找到用户，并且密码比对正确

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(jwtService, never()).generateToken(any(User.class));
        // 验证未验证邮箱时返回 403，而且不会生成 JWT
    }
}
