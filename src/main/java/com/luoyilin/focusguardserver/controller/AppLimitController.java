package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.model.AppLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/limits")
public class AppLimitController {

    private final List<AppLimit> appLimits = new ArrayList<>();
    // 在服务器内存中保存应用限制数据

    public AppLimitController() {

        // 程序启动时添加两条测试数据
        appLimits.add(new AppLimit(
                "com.instagram.android",
                30,
                true
        ));

        appLimits.add(new AppLimit(
                "tv.danmaku.bili",
                45,
                true
        ));
    }

    @GetMapping
    public List<AppLimit> getAllLimits() {

        // 返回内存中的全部应用限制
        return appLimits;
    }

    @PostMapping
    public AppLimit addLimit(@RequestBody AppLimit appLimit) {

        // 将客户端提交的应用限制加入内存列表
        appLimits.add(appLimit);

        // 把添加成功的数据返回给客户端
        return appLimit;
    }
}