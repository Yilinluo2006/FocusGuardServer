//访问数据库
package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.AppLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppLimitRepository
        extends JpaRepository<AppLimit, Long> {

    boolean existsByPackageName(String packageName);
    // 根据应用包名判断数据库中是否已经存在对应记录
    boolean existsByPackageNameAndIdNot(String packageName, long id);
    //判断指定包名是否被当前ID以外的其他记录使用
}