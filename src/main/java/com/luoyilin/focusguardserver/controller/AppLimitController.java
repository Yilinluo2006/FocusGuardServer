package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.service.AppLimitService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/limits")
public class AppLimitController {

    private final AppLimitService appLimitService;
    // 保存由 Spring 提供的 Service 业务对象

    public AppLimitController(AppLimitService appLimitService) {
        this.appLimitService = appLimitService;
    }
    // 构造器注入：Spring 自动传入 Service

    @GetMapping
    public List<AppLimit> getAllLimits() {
        return appLimitService.getAllLimits();
    }
    // 接收 GET /api/limits，查询全部限制

    @PostMapping
    public AppLimit addLimit(@RequestBody AppLimit appLimit) {
        return appLimitService.addLimit(appLimit);
    }
    // 接收 POST /api/limits，新增限制

    @PutMapping("/{id}")
    public AppLimit updateLimit(
            @PathVariable("id") Long id,
            @RequestBody AppLimit updatedAppLimit
    ) {
        return appLimitService.updateLimit(id, updatedAppLimit);
    }
    // 接收 PUT /api/limits/{id}，修改指定限制

    @DeleteMapping("/{id}")
    public void deleteLimit(@PathVariable("id") Long id) {
        appLimitService.deleteLimit(id);
    }
    // 接收 DELETE /api/limits/{id}，删除指定限制
}