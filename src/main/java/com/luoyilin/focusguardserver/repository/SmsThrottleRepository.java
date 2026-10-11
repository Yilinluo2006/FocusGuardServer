package com.luoyilin.focusguardserver.repository;
import com.luoyilin.focusguardserver.model.SmsThrottle;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface SmsThrottleRepository extends JpaRepository<SmsThrottle, String> {
    @Modifying @Query(value = "insert ignore into sms_throttles (scope_key, requested_at, window_started_at, request_count) values (:key,0,0,0)", nativeQuery = true)
    void ensure(@Param("key") String key);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from SmsThrottle s where s.scopeKey=:key")
    SmsThrottle lock(@Param("key") String key);
}
