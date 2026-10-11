package com.luoyilin.focusguardserver.repository;
import com.luoyilin.focusguardserver.model.DeviceDayUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface DeviceDayUsageRepository extends JpaRepository<DeviceDayUsage,Long> {
    List<DeviceDayUsage> findAllByUserIdAndDay(Long userId, LocalDate day);
    Optional<DeviceDayUsage> findByUserIdAndDeviceIdAndDay(Long userId,String device,LocalDate day);
}
