package com.luoyilin.focusguardserver.repository;
import com.luoyilin.focusguardserver.model.DeviceCompliance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface DeviceComplianceRepository extends JpaRepository<DeviceCompliance,Long> {
    Optional<DeviceCompliance> findByUserIdAndDeviceIdAndDay(Long user,String device,LocalDate day);
    List<DeviceCompliance> findAllByUserIdAndDay(Long user,LocalDate day);
}
