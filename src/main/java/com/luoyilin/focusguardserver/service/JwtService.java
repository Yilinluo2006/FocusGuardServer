package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    // 保存用于生成和验证 JWT 签名的密钥

    private final long expirationMs;
    // 保存 JWT 的有效时长，单位是毫秒

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }
    // 读取配置，将 Base64 密钥转换为 JJWT 使用的 SecretKey

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expirationTime = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject("user:" + user.getId())
                .claim("userId", user.getId())
                .claim("username", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(signingKey)
                .compact();
    }
    // 创建包含用户身份、签发时间和过期时间的 JWT

    public Long extractUserId(String token) {
        try {
            Object value = parseClaims(token).get("userId");
            return value instanceof Number && ((Number) value).longValue() > 0 ? ((Number) value).longValue() : null;
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }
    // 使用稳定的用户 ID 认证；旧版邮箱 subject 的令牌同样携带此字段。

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
    // 签名错误、内容损坏或已经过期时返回 false

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    // 使用相同密钥验证签名，并读取 JWT 中的 Claims
}
