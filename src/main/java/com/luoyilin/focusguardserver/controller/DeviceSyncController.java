package com.luoyilin.focusguardserver.controller;
import com.luoyilin.focusguardserver.dto.DeviceSyncData.*;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.DeviceSyncService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/sync")
public class DeviceSyncController {
    private final DeviceSyncService service;
    public DeviceSyncController(DeviceSyncService service){this.service=service;}
    @GetMapping("/settings") public Settings get(@AuthenticationPrincipal User user){return service.get(user);}
    @PutMapping("/settings") public Settings put(@AuthenticationPrincipal User user,@Valid @RequestBody SettingsRequest request){return service.put(user,request);}
    @GetMapping("/usage/{device}") public UsageResult usage(@AuthenticationPrincipal User user,@PathVariable String device,@RequestParam(required=false) LocalDate day){return service.result(user,day,device);}
    @PostMapping("/usage/{device}") public UsageResult post(@AuthenticationPrincipal User user,@PathVariable String device,@Valid @RequestBody UsageRequest request){return service.exchange(user,device,request);}
}
