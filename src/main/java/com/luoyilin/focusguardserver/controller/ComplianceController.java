package com.luoyilin.focusguardserver.controller;
import com.luoyilin.focusguardserver.dto.ComplianceData.*;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.ComplianceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sync/compliance")
public class ComplianceController {
    private final ComplianceService service;
    public ComplianceController(ComplianceService service){this.service=service;}
    @PostMapping("/{device}") public Result exchange(@AuthenticationPrincipal User user,@PathVariable String device,@Valid @RequestBody Request request){return service.exchange(user,device,request);}
}
