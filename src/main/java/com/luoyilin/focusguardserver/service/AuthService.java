package com.luoyilin.focusguardserver.service;

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
    // 负责查询和保存 users 表中的数据

    private final PasswordEncoder passwordEncoder;
    // 负责处理密码哈希

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    // Spring 通过构造器自动注入两个依赖对象

    public User register(RegisterRequest request) {
        String username = request.getUsername().trim();
        // 去掉用户名开头和结尾的空格

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);
        // 去掉邮箱两端空格，并统一转换成小写

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "用户名已经存在"
            );
        }
        // 用户名重复时返回 HTTP 409

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "邮箱已经被注册"
            );
        }
        // 邮箱重复时返回 HTTP 409

        String passwordHash =
                passwordEncoder.encode(request.getPassword());
        // 使用 BCrypt 把明文密码转换成不可逆的哈希值

        User user = new User(
                username,
                email,
                passwordHash
        );
        // 创建包含用户名、邮箱和密码哈希的用户实体

        return userRepository.save(user);
        // 将用户保存到 MySQL，并返回带有 id 的用户对象
    }
}