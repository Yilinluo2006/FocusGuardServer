package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.dto.auth.LoginRequest;
import com.luoyilin.focusguardserver.dto.auth.LoginResponse;
import com.luoyilin.focusguardserver.dto.auth.RegisterRequest;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    // Spring 自动注入数据库操作、密码处理和 JWT 服务

    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "邮箱已经被注册"
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        User user = User.createDefault(email, passwordHash);
        return userRepository.save(user);
    }
    // 检查注册信息、加密密码并保存用户

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "邮箱或密码错误"
                ));

        boolean passwordMatches = user.getPasswordHash() != null && passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "邮箱或密码错误"
            );
        }

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "请先完成注册邮箱验证"
            );
        }
        // 密码正确但邮箱尚未验证时，不生成 JWT

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user);
    }
    // 查询邮箱、比对 BCrypt 密码，成功后生成 JWT

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
    // 去掉邮箱两端空格并统一转换为小写
}
