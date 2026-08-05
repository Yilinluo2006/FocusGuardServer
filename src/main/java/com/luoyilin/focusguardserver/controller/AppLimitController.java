package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.AppLimitRequest;
import com.luoyilin.focusguardserver.dto.AppLimitResponse;
import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.AppLimitService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/limits")
public class AppLimitController {

    private final AppLimitService appLimitService;

    public AppLimitController(AppLimitService appLimitService) {
        this.appLimitService = appLimitService;
    }
    // Spring 自动注入 AppLimitService

    @GetMapping
    public List<AppLimitResponse> getAllLimits(
            @AuthenticationPrincipal User currentUser
    ) {
        return appLimitService.getAllLimits(currentUser)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }
    // 只返回当前登录用户的全部限制

    @GetMapping("/{id}")
    public AppLimitResponse getLimitById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        AppLimit appLimit =
                appLimitService.getLimitById(id, currentUser);

        return convertToResponse(appLimit);
    }
    // 只查询属于当前用户的指定记录

    @PostMapping
    public AppLimitResponse addLimit(
            @Valid @RequestBody AppLimitRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        AppLimit appLimit = convertToAppLimit(request);

        AppLimit savedAppLimit =
                appLimitService.addLimit(appLimit, currentUser);

        return convertToResponse(savedAppLimit);
    }
    // 新增记录时自动绑定当前登录用户

    @PutMapping("/{id}")
    public AppLimitResponse updateLimit(
            @PathVariable("id") Long id,
            @Valid @RequestBody AppLimitRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        AppLimit updatedAppLimit = convertToAppLimit(request);

        AppLimit savedAppLimit = appLimitService.updateLimit(
                id,
                updatedAppLimit,
                currentUser
        );

        return convertToResponse(savedAppLimit);
    }
    // 只能修改当前用户自己的记录

    @DeleteMapping("/{id}")
    public void deleteLimit(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        appLimitService.deleteLimit(id, currentUser);
    }
    // 只能删除当前用户自己的记录

    private AppLimit convertToAppLimit(AppLimitRequest request) {
        return new AppLimit(
                request.getPackageName(),
                request.getLimitMinutes(),
                request.isEnabled()
        );
    }
    // 把客户端请求 DTO 转换为数据库实体

    private AppLimitResponse convertToResponse(AppLimit appLimit) {
        return new AppLimitResponse(
                appLimit.getId(),
                appLimit.getPackageName(),
                appLimit.getLimitMinutes(),
                appLimit.isEnabled()
        );
    }
    // 返回 DTO，不向客户端暴露 User 和密码等内部数据
}