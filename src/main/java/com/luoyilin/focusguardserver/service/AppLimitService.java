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
    // 保存由 Spring 提供的 Repository 数据访问对象

    public AppLimitService(AppLimitRepository appLimitRepository) {
        this.appLimitRepository = appLimitRepository;
    }
    // 构造器注入：Spring 自动传入 Repository 对象

    public List<AppLimit> getAllLimits() {
        return appLimitRepository.findAll();
    }
    // 从 MySQL 查询全部应用限制

    public AppLimit addLimit(AppLimit appLimit) {
        return appLimitRepository.save(appLimit);
    }
    // 把新的应用限制保存到 MySQL

    public AppLimit updateLimit(Long id, AppLimit updatedAppLimit) {

        AppLimit existingAppLimit = appLimitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "App limit not found"
                ));
        // 根据 id 查询原记录；不存在时返回 404

        existingAppLimit.setPackageName(updatedAppLimit.getPackageName());
        existingAppLimit.setLimitMinutes(updatedAppLimit.getLimitMinutes());
        existingAppLimit.setEnabled(updatedAppLimit.isEnabled());
        // 使用客户端提交的数据修改原对象

        return appLimitRepository.save(existingAppLimit);
        // 保存修改后的对象，Hibernate 会执行 UPDATE
    }

    public void deleteLimit(Long id) {

        if (!appLimitRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "App limit not found"
            );
        }
        // 删除前检查记录是否存在；不存在时返回 404

        appLimitRepository.deleteById(id);
        // 根据 id 从 MySQL 删除对应记录
    }
}