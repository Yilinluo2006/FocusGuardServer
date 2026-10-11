package com.luoyilin.focusguardserver.controller;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.SleepTemporaryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sync/sleep/temporary/{device}")
public class SleepTemporaryController {
    private final SleepTemporaryService service;
    public SleepTemporaryController(SleepTemporaryService service){this.service=service;}
    @GetMapping public SleepTemporaryService.State get(@AuthenticationPrincipal User user,@PathVariable String device){return service.get(user,device);}
    @PostMapping public SleepTemporaryService.State grant(@AuthenticationPrincipal User user,@PathVariable String device,
            @Valid @RequestBody SleepTemporaryService.Request request){return service.grant(user,device,request);}
}
