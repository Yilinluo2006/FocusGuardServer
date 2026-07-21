package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.service.AppLimitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/limits")
public class AppLimitController {

    private final AppLimitService appLimitService;
    // 保存 Spring 提供的 AppLimitService 对象

    public AppLimitController(AppLimitService appLimitService) {
        this.appLimitService = appLimitService;
    }
    // 构造器注入：Spring 创建 Controller 时自动传入 Service

    @GetMapping
    public List<AppLimit> getAllLimits() {

        // 调用 Service 查询全部应用限制
        return appLimitService.getAllLimits();
    }

    @PostMapping
    public AppLimit addLimit(@RequestBody AppLimit appLimit) {

        // 将客户端提交的数据交给 Service 处理
        return appLimitService.addLimit(appLimit);
    }
}