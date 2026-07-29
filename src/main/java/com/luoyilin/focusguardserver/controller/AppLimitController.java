package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.service.AppLimitService;
import jakarta.validation.Valid;
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
    // 保存由 Spring 提供的 Service

    public AppLimitController(AppLimitService appLimitService) {
        this.appLimitService = appLimitService;
    }
    // 通过构造器注入 Service

    @GetMapping
    public List<AppLimit> getAllLimits() {
        return appLimitService.getAllLimits();
    }
    // GET /api/limits：查询全部限制

    @GetMapping("/{id}")
    public AppLimit getLimitById(@PathVariable("id") Long id) {
        return appLimitService.getLimitById(id);
    }
    // GET /api/limits/{id}：查询单条限制

    @PostMapping
    public AppLimit addLimit(@Valid @RequestBody AppLimit appLimit) {
        return appLimitService.addLimit(appLimit);
    }
    // POST /api/limits：校验数据并新增限制

    @PutMapping("/{id}")
    public AppLimit updateLimit(
            @PathVariable("id") Long id,
            @Valid @RequestBody AppLimit updatedAppLimit
    ) {
        return appLimitService.updateLimit(id, updatedAppLimit);
    }
    // PUT /api/limits/{id}：校验数据并修改限制

    @DeleteMapping("/{id}")
    public void deleteLimit(@PathVariable("id") Long id) {
        appLimitService.deleteLimit(id);
    }
    // DELETE /api/limits/{id}：删除限制
}