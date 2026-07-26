//访问数据库
package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.AppLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppLimitRepository
        extends JpaRepository<AppLimit, Long> {
}