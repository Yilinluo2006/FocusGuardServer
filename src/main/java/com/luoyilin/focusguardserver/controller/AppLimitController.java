package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.AppLimitRequest;
import com.luoyilin.focusguardserver.dto.AppLimitResponse;
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
    // 保存由 Spring 提供的 AppLimitService 对象

    public AppLimitController(AppLimitService appLimitService) {
        this.appLimitService = appLimitService;
    }
    // 构造器注入：Spring 创建 Controller 时自动传入 Service

    @GetMapping
    public List<AppLimitResponse> getAllLimits() {
        return appLimitService.getAllLimits()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }
    // 查询全部实体，并把每个 AppLimit 转换成 AppLimitResponse

    @GetMapping("/{id}")
    public AppLimitResponse getLimitById(
            @PathVariable("id") Long id
    ) {
        AppLimit appLimit = appLimitService.getLimitById(id);
        // 通过 Service 查询数据库实体

        return convertToResponse(appLimit);
        // 把实体转换成响应 DTO 后再返回
    }
    // GET /api/limits/{id}：查询指定应用限制

    @PostMapping
    public AppLimitResponse addLimit(
            @Valid @RequestBody AppLimitRequest request
    ) {
        AppLimit appLimit = convertToAppLimit(request);
        // 把请求 DTO 转换成数据库实体

        AppLimit savedAppLimit = appLimitService.addLimit(appLimit);
        // 保存数据并取得包含数据库 ID 的实体

        return convertToResponse(savedAppLimit);
        // 把保存结果转换成响应 DTO
    }
    // POST /api/limits：校验并新增应用限制

    @PutMapping("/{id}")
    public AppLimitResponse updateLimit(
            @PathVariable("id") Long id,
            @Valid @RequestBody AppLimitRequest request
    ) {
        AppLimit updatedAppLimit = convertToAppLimit(request);
        // 把客户端的新数据转换成实体

        AppLimit savedAppLimit =
                appLimitService.updateLimit(id, updatedAppLimit);
        // 更新数据库中指定 ID 的记录

        return convertToResponse(savedAppLimit);
        // 把更新后的实体转换成响应 DTO
    }
    // PUT /api/limits/{id}：校验并修改应用限制

    @DeleteMapping("/{id}")
    public void deleteLimit(@PathVariable("id") Long id) {
        appLimitService.deleteLimit(id);
    }
    // DELETE /api/limits/{id}：删除指定应用限制

    private AppLimit convertToAppLimit(AppLimitRequest request) {
        return new AppLimit(
                request.getPackageName(),
                request.getLimitMinutes(),
                request.isEnabled()
        );
    }
    // AppLimitRequest 转换为数据库实体 AppLimit

    private AppLimitResponse convertToResponse(AppLimit appLimit) {
        return new AppLimitResponse(
                appLimit.getId(),
                appLimit.getPackageName(),
                appLimit.getLimitMinutes(),
                appLimit.isEnabled()
        );
    }
    // 数据库实体 AppLimit 转换为 AppLimitResponse
}