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
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.email = :email")
    Optional<User> lockByEmail(@org.springframework.data.repository.query.Param("email") String email);
    // 根据邮箱查找用户，之后邮箱登录时会使用
}
