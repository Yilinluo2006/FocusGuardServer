package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.User;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.SecretKey;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test void oldEmailSubjectStillAuthenticatesBySignedUserId() {
        SecretKey key = Jwts.SIG.HS256.key().build();
        JwtService service = new JwtService(Base64.getEncoder().encodeToString(key.getEncoded()), 60000);
        String oldToken = Jwts.builder().subject("old@example.invalid").claim("userId", 42L).signWith(key).compact();
        assertEquals(42L, service.extractUserId(oldToken));
        User user = User.createDefault("new@example.invalid", "unused");
        ReflectionTestUtils.setField(user, "id", 42L); user.updateProfile("another-name", "sad");
        assertEquals(42L, service.extractUserId(service.generateToken(user)));
    }
    @Test void rejectsInvalidTokenAndDoesNotUseEmailAsIdentityFallback() {
        SecretKey key = Jwts.SIG.HS256.key().build();
        JwtService service = new JwtService(Base64.getEncoder().encodeToString(key.getEncoded()), 60000);
        assertNull(service.extractUserId("invalid"));
        assertNull(service.extractUserId(Jwts.builder().subject("email@example.invalid").signWith(key).compact()));
        assertNull(service.extractUserId(Jwts.builder().claim("userId", -1).signWith(key).compact()));
    }
}
