package com.luoyilin.focusguardserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> checkHealth() {

        // 返回后端服务的运行状态，Spring 会自动转换成 JSON
        return Map.of(
                "status", "ok",
                "service", "FocusGuardServer"
        );
    }
}