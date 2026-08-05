//访问数据库
package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppLimitRepository
        extends JpaRepository<AppLimit, Long> {

    List<AppLimit> findAllByUser(User user);
    // 查询指定用户拥有的所有应用限制

    Optional<AppLimit> findByIdAndUser(Long id, User user);
    // 根据限制记录 id 查询，并确保记录属于指定用户

    boolean existsByPackageNameAndUser(
            String packageName,
            User user
    );
    // 检查当前用户是否已经添加过这个应用

    boolean existsByPackageNameAndUserAndIdNot(
            String packageName,
            User user,
            Long id
    );
    // 更新记录时，排除当前记录，再检查包名是否重复
}