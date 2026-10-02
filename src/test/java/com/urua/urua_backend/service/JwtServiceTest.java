package com.urua.urua_backend.service;

import com.urua.urua_backend.model.Role;
import com.urua.urua_backend.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha-256";

    private User sampleUser() {
        return User.builder()
                .id("u1")
                .email("buyer@test.com")
                .role(Role.BUYER)
                .build();
    }

    @Test
    void generatedToken_isValid_andContainsTheUsersEmail() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken(sampleUser());

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("buyer@test.com", jwtService.extractEmail(token));
    }

    @Test
    void tokenSignedWithADifferentSecret_isRejected() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService otherServer = new JwtService("a-completely-different-secret-key-for-testing-1234", 60_000);

        String token = issuer.generateToken(sampleUser());

        assertFalse(otherServer.isTokenValid(token));
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwtService = new JwtService(SECRET, -1_000);

        String token = jwtService.generateToken(sampleUser());

        assertFalse(jwtService.isTokenValid(token));
    }
}