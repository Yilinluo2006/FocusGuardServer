package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.CloudBackupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/backups")
public class CloudBackupController {
    private final CloudBackupService service;
    public CloudBackupController(CloudBackupService service) { this.service = service; }
    @GetMapping public List<BackupResponse> list(@AuthenticationPrincipal User user) { return service.list(user); }
    @GetMapping("/{device}") public BackupResponse get(@AuthenticationPrincipal User user, @PathVariable String device) { return service.get(user, device); }
    @PutMapping("/{device}") public BackupResponse put(@AuthenticationPrincipal User user, @PathVariable String device,
            @Valid @RequestBody BackupRequest request) { return service.put(user, device, request); }
}
