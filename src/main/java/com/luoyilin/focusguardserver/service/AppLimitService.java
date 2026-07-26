package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.repository.AppLimitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppLimitService {

    private final AppLimitRepository appLimitRepository;
    // 保存由 Spring 提供的 Repository 数据访问对象

    public AppLimitService(AppLimitRepository appLimitRepository) {
        this.appLimitRepository = appLimitRepository;
    }
    // 构造器注入：Spring 自动创建并传入 Repository 对象

    public List<AppLimit> getAllLimits() {
        return appLimitRepository.findAll();
    }
    // 从 MySQL 的 app_limits 表中查询全部记录

    public AppLimit addLimit(AppLimit appLimit) {
        return appLimitRepository.save(appLimit);
    }
    // 将新的应用限制保存到 MySQL，并返回保存后的对象
}