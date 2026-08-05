package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.AppLimitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AppLimitService {

    private final AppLimitRepository appLimitRepository;

    public AppLimitService(AppLimitRepository appLimitRepository) {
        this.appLimitRepository = appLimitRepository;
    }
    // 通过构造器获得数据库访问对象

    public List<AppLimit> getAllLimits(User user) {
        return appLimitRepository.findAllByUser(user);
    }
    // 只查询当前用户的应用限制

    public AppLimit getLimitById(Long id, User user) {
        return appLimitRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "未找到该应用限制"
                ));
    }
    // 记录不存在或不属于当前用户时，都返回 404

    public AppLimit addLimit(AppLimit appLimit, User user) {
        boolean alreadyExists =
                appLimitRepository.existsByPackageNameAndUser(
                        appLimit.getPackageName(),
                        user
                );

        if (alreadyExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "该用户已经存在这个应用的限制记录"
            );
        }

        appLimit.setUser(user);
        return appLimitRepository.save(appLimit);
    }
    // 保存前绑定当前用户，防止产生没有所有者的数据

    public AppLimit updateLimit(
            Long id,
            AppLimit updatedAppLimit,
            User user
    ) {
        AppLimit existingAppLimit = getLimitById(id, user);

        boolean usedByAnotherRecord =
                appLimitRepository.existsByPackageNameAndUserAndIdNot(
                        updatedAppLimit.getPackageName(),
                        user,
                        id
                );

        if (usedByAnotherRecord) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "该用户已经存在这个应用的其他限制记录"
            );
        }

        existingAppLimit.setPackageName(updatedAppLimit.getPackageName());
        existingAppLimit.setLimitMinutes(updatedAppLimit.getLimitMinutes());
        existingAppLimit.setEnabled(updatedAppLimit.isEnabled());

        return appLimitRepository.save(existingAppLimit);
    }
    // 只能修改属于当前用户的记录，并保持原来的用户归属

    public void deleteLimit(Long id, User user) {
        AppLimit existingAppLimit = getLimitById(id, user);
        appLimitRepository.delete(existingAppLimit);
    }
    // 只能删除属于当前用户的记录
}