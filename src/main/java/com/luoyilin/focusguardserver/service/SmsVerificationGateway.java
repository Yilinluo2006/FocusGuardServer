package com.luoyilin.focusguardserver.service;

/** Provider-only proof verification; codes must never be returned to an app. */
public interface SmsVerificationGateway {
    void send(String phone, String scheme, String template);
    boolean verify(String phone, String scheme, String code);
}
