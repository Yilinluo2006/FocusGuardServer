package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    // 检查数据库中是否已经存在指定用户名

    boolean existsByEmail(String email);
    // 检查数据库中是否已经存在指定邮箱

    Optional<User> findByUsername(String username);
    // 根据用户名查找用户，没有找到时返回 Optional.empty()

    Optional<User> findByEmail(String email);
    // 根据邮箱查找用户，之后邮箱登录时会使用
}