package com.luoyilin.focusguardserver.controller;
import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.dto.auth.LoginResponse;
import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.service.PhoneAccountService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
public class PhoneController {
    private final PhoneAccountService phones;
    public PhoneController(PhoneAccountService phones) { this.phones=phones; }
    @PostMapping("/api/auth/phone/request") public PhoneCodeResponse loginCode(@Valid @RequestBody PhoneCodeRequest r,HttpServletRequest request) { return phones.requestLogin(r,request.getRemoteAddr()); }
    @PostMapping("/api/auth/phone/confirm") public LoginResponse login(@Valid @RequestBody PhoneCodeConfirm r) { return phones.login(r); }
    @PostMapping("/api/account/bindings/phone/proof/request") public PhoneCodeResponse proof(@AuthenticationPrincipal User user,HttpServletRequest request) { return phones.requestProof(user,request.getRemoteAddr()); }
    @PostMapping("/api/account/bindings/phone/request") public PhoneCodeResponse bindCode(@AuthenticationPrincipal User user,@Valid @RequestBody PhoneBindingRequest r,HttpServletRequest request) { return phones.requestBinding(user,r,request.getRemoteAddr()); }
    @PostMapping("/api/account/bindings/phone/confirm") public ProfileResponse bind(@AuthenticationPrincipal User user,@Valid @RequestBody PhoneCodeConfirm r) { return phones.confirmBinding(user,r); }
}
