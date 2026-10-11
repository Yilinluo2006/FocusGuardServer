package com.luoyilin.focusguardserver.controller;

import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/account")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts = accounts; }
    @GetMapping("/profile") public ProfileResponse profile(@AuthenticationPrincipal User user) { return accounts.profile(user); }
    @PutMapping("/profile") public ProfileResponse update(@AuthenticationPrincipal User user, @Valid @RequestBody ProfileRequest request) {
        return accounts.update(user, request);
    }
    @PostMapping("/bindings/email/request") public Map<String, String> request(@AuthenticationPrincipal User user, @Valid @RequestBody EmailBindingRequest request) {
        accounts.requestEmail(user, request); return Map.of("message", "换绑验证码已提交发送");
    }
    @PostMapping("/bindings/email/confirm") public ProfileResponse confirm(@AuthenticationPrincipal User user, @Valid @RequestBody EmailBindingConfirm request) {
        return accounts.confirmEmail(user, request);
    }
}
