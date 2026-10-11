package com.luoyilin.focusguardserver.repository;
import com.luoyilin.focusguardserver.model.WechatChallenge;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.Instant;
public interface WechatChallengeRepository extends JpaRepository<WechatChallenge,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from WechatChallenge c where c.tokenHash=:hash")
    Optional<WechatChallenge> lockByHash(@Param("hash")String hash);
    void deleteByExpiresAtBefore(Instant time);
}
