package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.PasswordResetCode;
import com.luoyilin.focusguardserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository
        extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findByUser(User user);
    // 查询指定用户当前保存的密码重置验证码记录

    boolean existsByUserAndCreatedAtAfter(
            User user,
            LocalDateTime createdAfter
    );
    // 判断该用户在指定时间之后是否已经申请过验证码

    void deleteByUser(User user);
    // 生成新验证码前删除该用户原来的验证码记录
}
