package com.luoyilin.focusguardserver.dto;
public record PhoneCodeResponse(String challengeId, int resendAfterSeconds, int expiresInSeconds, String maskedPhone) { }
