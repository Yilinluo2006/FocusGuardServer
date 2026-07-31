package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.repository.AppLimitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AppLimitService {

    private final AppLimitRepository appLimitRepository;
    // 保存由 Spring 提供的 Repository 对象

    public AppLimitService(AppLimitRepository appLimitRepository) {
        this.appLimitRepository = appLimitRepository;
    }
    // 构造器注入：Spring 自动传入 Repository

    public List<AppLimit> getAllLimits() {
        return appLimitRepository.findAll();
    }
    // 查询全部应用限制

    public AppLimit getLimitById(Long id) {
        return appLimitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "App limit not found"
                ));
    }
    // 根据 id 查询记录，不存在时返回 404

    public AppLimit addLimit(AppLimit appLimit) {
        boolean alreadyExists =
                appLimitRepository.existsByPackageName(
                        appLimit.getPackageName()
                );
        // 判断数据库中是否已经存在相同包名

        if (alreadyExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "该应用已经存在限制记录"
            );
        }
        // 新增时发现相同包名，返回 HTTP 409

        return appLimitRepository.save(appLimit);
        // 包名不重复时保存到 MySQL
    }

    public AppLimit updateLimit(
            Long id,
            AppLimit updatedAppLimit
    ) {
        AppLimit existingAppLimit = getLimitById(id);
        // 查询准备修改的原记录，不存在时返回 404

        boolean usedByAnotherRecord =
                appLimitRepository.existsByPackageNameAndIdNot(
                        updatedAppLimit.getPackageName(),
                        id
                );
        // 检查目标包名是否被当前 ID 以外的记录使用

        if (usedByAnotherRecord) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "该应用已经存在其他限制记录"
            );
        }
        // 如果其他记录已经使用该包名，则返回 HTTP 409

        existingAppLimit.setPackageName(
                updatedAppLimit.getPackageName()
        );

        existingAppLimit.setLimitMinutes(
                updatedAppLimit.getLimitMinutes()
        );

        existingAppLimit.setEnabled(
                updatedAppLimit.isEnabled()
        );
        // 检查通过后，把客户端的新数据写入原实体

        return appLimitRepository.save(existingAppLimit);
        // 保存修改，Hibernate 会执行 UPDATE
    }

    public void deleteLimit(Long id) {
        if (!appLimitRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "App limit not found"
            );
        }
        // 要删除的记录不存在时返回 404

        appLimitRepository.deleteById(id);
        // 从 MySQL 中删除指定记录
    }
}