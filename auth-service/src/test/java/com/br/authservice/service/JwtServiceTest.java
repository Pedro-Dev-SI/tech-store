package com.br.authservice.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void generateAndValidateAccessToken() {
        // 32+ chars secret for HS256
        String secret = "01234567890123456789012345678901";
        JwtService jwtService = new JwtService(secret, 1000, 2000);

        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@email.com", "USER");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.getUserId(token));
    }
}
