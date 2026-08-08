package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.EmailVerificationCode;
import com.luoyilin.focusguardserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeRepository
        extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByUser(User user);
    // 查询指定用户当前保存的邮箱验证码

    boolean existsByUserAndCreatedAtAfter(
            User user,
            LocalDateTime createdAfter
    );
    // 判断指定用户在某个时间之后是否已经申请过验证码

    void deleteByUser(User user);
    // 生成新验证码前删除该用户原来的验证码记录
}
