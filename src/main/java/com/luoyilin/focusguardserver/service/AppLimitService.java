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
    // 保存由 Spring 提供的 Repository

    public AppLimitService(AppLimitRepository appLimitRepository) {
        this.appLimitRepository = appLimitRepository;
    }
    // 通过构造器注入 Repository

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
    // 根据 id 查询单条记录；不存在则返回 404

    public AppLimit addLimit(AppLimit appLimit) {
        return appLimitRepository.save(appLimit);
    }
    // 新增应用限制

    public AppLimit updateLimit(Long id, AppLimit updatedAppLimit) {
        AppLimit existingAppLimit = getLimitById(id);
        // 复用查询方法，获取原记录或返回 404

        existingAppLimit.setPackageName(updatedAppLimit.getPackageName());
        existingAppLimit.setLimitMinutes(updatedAppLimit.getLimitMinutes());
        existingAppLimit.setEnabled(updatedAppLimit.isEnabled());
        // 把新数据写入原对象

        return appLimitRepository.save(existingAppLimit);
        // 保存修改，Hibernate 执行 UPDATE
    }

    public void deleteLimit(Long id) {
        if (!appLimitRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "App limit not found"
            );
        }
        // 不存在时返回 404

        appLimitRepository.deleteById(id);
        // 从 MySQL 删除记录
    }
}