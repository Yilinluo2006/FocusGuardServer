package com.luoyilin.focusguardserver.repository;
import com.luoyilin.focusguardserver.model.PhoneChallenge;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface PhoneChallengeRepository extends JpaRepository<PhoneChallenge, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from PhoneChallenge c where c.id=:id")
    Optional<PhoneChallenge> lockById(@Param("id") String id);
    List<PhoneChallenge> findByPhoneAndPurposeAndOwnerIdAndUsedFalse(String phone, PhoneChallenge.Purpose purpose, Long ownerId);
}
