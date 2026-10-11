package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record WechatCodeRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,256}") String code,
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String state) { }
