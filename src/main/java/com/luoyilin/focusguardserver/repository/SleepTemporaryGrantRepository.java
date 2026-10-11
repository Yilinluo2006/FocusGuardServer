package com.luoyilin.focusguardserver.repository;
import com.luoyilin.focusguardserver.model.SleepTemporaryGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface SleepTemporaryGrantRepository extends JpaRepository<SleepTemporaryGrant,Long> {
    List<SleepTemporaryGrant> findAllByUserIdAndDay(Long user,LocalDate day);
    Optional<SleepTemporaryGrant> findByUserIdAndRequestId(Long user,String request);
}
