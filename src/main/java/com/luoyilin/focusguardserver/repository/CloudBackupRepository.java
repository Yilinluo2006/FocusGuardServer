package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.CloudBackup;
import com.luoyilin.focusguardserver.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface CloudBackupRepository extends JpaRepository<CloudBackup, Long> {
    Optional<CloudBackup> findByUserAndDeviceId(User user, String deviceId);
    List<CloudBackup> findAllByUserOrderByUpdatedAtDesc(User user);
    // Lock the owner even on first insert, so concurrent device requests cannot race.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockOwner(@Param("id") Long id);
}
