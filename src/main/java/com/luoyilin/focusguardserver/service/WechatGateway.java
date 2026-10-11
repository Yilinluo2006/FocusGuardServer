package com.luoyilin.focusguardserver.service;
public interface WechatGateway {
    record Verified(String openId,String unionId,String nickname){}
    boolean available();
    String appId();
    Verified exchange(String code);
}
