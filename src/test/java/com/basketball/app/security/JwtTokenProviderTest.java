package com.basketball.app.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class JwtTokenProviderTest {

    @Test
    void generateAndParseToken_ReturnsUsernameAndRole() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "short-secret");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60_000L);

        String token = provider.generateToken("coach@example.com", "TRAINER");

        assertTrue(provider.validateToken(token));
        assertEquals("coach@example.com", provider.getUsernameFromToken(token));
        assertEquals("TRAINER", provider.getRoleFromToken(token));
        assertFalse(provider.isTokenExpired(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "another-secret");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60_000L);

        assertFalse(provider.validateToken("not-a-jwt"));
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "long-enough-secret-for-hs256-algo");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 1L);

        String token = provider.generateToken("user@example.com", "PLAYER");
        Thread.sleep(5);

        assertFalse(provider.validateToken(token));
    }

    @Test
    void getExpirationDateFromToken_ReturnsFutureDate() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "long-enough-secret-for-hs256-algo");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3600_000L);

        String token = provider.generateToken("admin@example.com", "ADMIN");

        assertNotNull(provider.getExpirationDateFromToken(token));
        assertFalse(provider.getExpirationDateFromToken(token).before(new java.util.Date()));
    }

    @Test
    void generateToken_ShortSecret_PadsToMinimumLength() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "tiny");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60_000L);

        String token = provider.generateToken("coach@example.com", "COACH");

        assertTrue(provider.validateToken(token));
        assertEquals("coach@example.com", provider.getUsernameFromToken(token));
    }
}
