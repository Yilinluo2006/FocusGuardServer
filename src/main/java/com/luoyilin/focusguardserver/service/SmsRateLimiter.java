package com.luoyilin.focusguardserver.service;
import com.luoyilin.focusguardserver.repository.SmsThrottleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@Service
public class SmsRateLimiter {
    private final SmsThrottleRepository gates;
    public SmsRateLimiter(SmsThrottleRepository gates) { this.gates = gates; }
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public void reserve(String phone, String remoteAddress) {
        long now = System.currentTimeMillis();
        String ip;
        try { ip = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(remoteAddress.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Unable to hash address"); }
        take("ip:" + ip, now, 3_600_000L, 2000L, 20);
        take("phone:" + phone, now, 86_400_000L, 60_000L, 10);
    }
    private void take(String key, long now, long window, long interval, int limit) {
        gates.ensure(key);
        var gate = gates.lock(key);
        if (!gate.reserve(now, window, interval, limit))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "短信请求过于频繁，请稍后重试");
        gates.saveAndFlush(gate);
    }
}
