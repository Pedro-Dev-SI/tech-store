package com.br.authservice.service;

import com.br.authservice.client.UserServiceClient;
import com.br.authservice.model.RefreshToken;
import com.br.authservice.repository.RefreshTokenRepository;
import com.br.authservice.service.dto.AuthResponse;
import com.br.authservice.service.dto.LoginRequest;
import com.br.authservice.service.dto.LogoutRequest;
import com.br.authservice.service.dto.RefreshTokenRequest;
import com.br.authservice.service.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @Test
    void register_success_returnsTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@email.com");
        request.setCpf("52998224725");
        request.setPhone("11999999999");
        request.setName("User");
        request.setPassword("SenhaForte1");

        UUID userId = UUID.randomUUID();
        when(userServiceClient.createUser(any()))
            .thenReturn(new UserServiceClient.UserResponse(userId, "user@email.com", "USER"));
        when(jwtService.generateAccessToken(userId, "user@email.com", "USER")).thenReturn("access");
        when(jwtService.generateRefreshToken(userId)).thenReturn("refresh");
        when(refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("refresh", saved.getToken());
    }

    @Test
    void login_success_returnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@email.com");
        request.setPassword("SenhaForte1");

        UUID userId = UUID.randomUUID();
        when(userServiceClient.validateLogin(any()))
            .thenReturn(new UserServiceClient.LoginResponse(userId, "user@email.com", "USER", "ACTIVE"));
        when(jwtService.generateAccessToken(userId, "user@email.com", "USER")).thenReturn("access");
        when(jwtService.generateRefreshToken(userId)).thenReturn("refresh");
        when(refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(request);

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
    }

    @Test
    void refresh_valid_returnsNewAccessToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken("refresh");
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));
        when(jwtService.generateAccessToken(userId, null, null)).thenReturn("new-access");

        AuthResponse response = authService.refresh(new RefreshTokenRequest() {{
            setRefreshToken("refresh");
        }});

        assertEquals("new-access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
    }

    @Test
    void refresh_revoked_throws() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> authService.refresh(new RefreshTokenRequest() {{
            setRefreshToken("refresh");
        }}));
    }

    @Test
    void refresh_expired_throws() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> authService.refresh(new RefreshTokenRequest() {{
            setRefreshToken("refresh");
        }}));
    }

    @Test
    void logout_revokesToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh");

        authService.logout(request);

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertNotNull(saved);
        assertEquals(true, saved.getRevoked());
    }
}
