package com.luoyilin.focusguardserver.controller;
import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.dto.auth.LoginResponse;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController public class WechatAuthController {
    private final WechatChallengeService challenges;private final WechatGateway gateway;private final WechatAccountTransactions accounts;private final JwtService jwt;private final AccountService profiles;
    public WechatAuthController(WechatChallengeService challenges,WechatGateway gateway,WechatAccountTransactions accounts,JwtService jwt,AccountService profiles){this.challenges=challenges;this.gateway=gateway;this.accounts=accounts;this.jwt=jwt;this.profiles=profiles;}
    @PostMapping("/api/auth/wechat/start") public WechatChallengeService.Start start(HttpServletRequest request){return challenges.start(WechatChallenge.Purpose.LOGIN,null,request.getRemoteAddr());}
    @PostMapping("/api/account/bindings/wechat/start") public WechatChallengeService.Start startBind(@AuthenticationPrincipal User user,HttpServletRequest request){return challenges.start(WechatChallenge.Purpose.BIND,user.getId(),request.getRemoteAddr());}
    @PostMapping("/api/auth/wechat/login") public LoginResponse login(@Valid @RequestBody WechatCodeRequest request){
        challenges.consume(request.state(),WechatChallenge.Purpose.LOGIN,null);
        try{User user=accounts.login(gateway.exchange(request.code()));return new LoginResponse(jwt.generateToken(user),user);}
        catch(DataIntegrityViolationException e){throw conflict();}
    }
    @PostMapping("/api/account/bindings/wechat/confirm") public ProfileResponse bind(@AuthenticationPrincipal User user,@Valid @RequestBody WechatCodeRequest request){
        challenges.consume(request.state(),WechatChallenge.Purpose.BIND,user.getId());
        try{return profiles.profile(accounts.bind(user.getId(),gateway.exchange(request.code())));}
        catch(DataIntegrityViolationException e){throw conflict();}
    }
    private ResponseStatusException conflict(){return new ResponseStatusException(HttpStatus.CONFLICT,"微信绑定状态已变化，请重新登录原账号后操作");}
}
