package com.br.authservice.service;

import com.br.authservice.client.UserServiceClient;
import com.br.authservice.model.RefreshToken;
import com.br.authservice.repository.RefreshTokenRepository;
import com.br.authservice.service.dto.AuthResponse;
import com.br.authservice.service.dto.LoginRequest;
import com.br.authservice.service.dto.LogoutRequest;
import com.br.authservice.service.dto.RefreshTokenRequest;
import com.br.authservice.service.dto.RegisterRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    public AuthService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService, UserServiceClient userServiceClient) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Registers a user in user-service and returns tokens.
     */
    public AuthResponse register(RegisterRequest request) {
        // Call user-service to create user (public endpoint)
        // This is intentionally minimal: the user-service enforces the rules.
        var createdUser = userServiceClient.createUser(request);

        if (createdUser == null || createdUser.id() == null) {
            throw new IllegalStateException("Could not create user");
        }

        // Generate tokens for the newly created user
        String accessToken = jwtService.generateAccessToken(createdUser.id(), createdUser.email(), createdUser.role());
        String refreshToken = jwtService.generateRefreshToken(createdUser.id());

        // Persist refresh token for session control
        saveRefreshToken(createdUser.id(), refreshToken);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Logs in the user and returns tokens.
     * Note: password check should be delegated to user-service (not implemented yet).
     */
    public AuthResponse login(LoginRequest request) {
        // Validate credentials via user-service
        var loginResponse = userServiceClient.validateLogin(request);

        if (loginResponse == null || loginResponse.id() == null) {
            throw new IllegalStateException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(
            loginResponse.id(),
            loginResponse.email(),
            loginResponse.role()
        );
        String refreshToken = jwtService.generateRefreshToken(loginResponse.id());

        saveRefreshToken(loginResponse.id(), refreshToken);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // Reject revoked tokens
        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new IllegalArgumentException("Refresh token revoked");
        }

        // Reject expired tokens
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        UUID userId = token.getUserId();
        // Access token regenerated; refresh token remains the same
        String accessToken = jwtService.generateAccessToken(userId, null, null);

        return new AuthResponse(accessToken, request.getRefreshToken());
    }

    /**
     * Revokes a refresh token (logout).
     */
    public void logout(LogoutRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // Mark refresh token as revoked
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Validates access token.
     */
    public boolean validate(String token) {
        return jwtService.isTokenValid(token);
    }

    private void saveRefreshToken(UUID userId, String token) {
        // Enforce max 5 active refresh tokens per user
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtAsc(userId);
        if (activeTokens.size() >= 5) {
            RefreshToken oldest = activeTokens.get(0);
            oldest.setRevoked(true);
            refreshTokenRepository.save(oldest);
        }

        // Persist the new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    
}
