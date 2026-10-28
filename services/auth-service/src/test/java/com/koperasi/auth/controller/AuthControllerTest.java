package com.koperasi.auth.controller;

import com.koperasi.auth.domain.dto.ApiResponse;
import com.koperasi.auth.domain.dto.LoginRequest;
import com.koperasi.auth.domain.dto.LoginResponse;
import com.koperasi.auth.domain.dto.RefreshRequest;
import com.koperasi.auth.domain.dto.RegisterRequest;
import com.koperasi.auth.domain.dto.UserResponse;
import com.koperasi.auth.domain.enums.RoleName;
import com.koperasi.auth.domain.enums.UserStatus;
import com.koperasi.auth.security.JwtPrincipal;
import com.koperasi.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private com.koperasi.auth.service.PasswordResetService passwordResetService;

    private AuthController authController;
    private MockHttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, passwordResetService);
        httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");
        httpRequest.addHeader("User-Agent", "JUnit-Agent");
    }

    @Test
    void register_returnsCreatedResponseAndDelegatesWithClientContext() {
        UserResponse userResponse = userResponse();
        when(authService.register(any(RegisterRequest.class), anyString(), anyString(), anyString()))
                .thenReturn(userResponse);
        RegisterRequest request = new RegisterRequest("user@example.id", "Koperasi#2026Aman", "+6281234567890", RoleName.BORROWER);

        ResponseEntity<ApiResponse<UserResponse>> response = authController.register(request, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEqualTo(userResponse);
        assertThat(response.getBody().message()).contains("Registrasi berhasil");

        ArgumentCaptor<String> deviceInfo = ArgumentCaptor.forClass(String.class);
        verify(authService).register(eq(request), deviceInfo.capture(), eq("127.0.0.1"), eq("JUnit-Agent"));
        assertThat(deviceInfo.getValue()).contains("127.0.0.1").contains("JUnit-Agent");
    }

    @Test
    void login_returnsOkResponse() {
        LoginResponse loginResponse = new LoginResponse("access", "refresh", "Bearer", 900);
        when(authService.login(any(LoginRequest.class), anyString(), anyString(), anyString()))
                .thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(
                new LoginRequest("user@example.id", "Koperasi#2026Aman"), httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("access");
        assertThat(response.getBody().message()).isEqualTo("Login berhasil");
    }

    @Test
    void refresh_returnsOkResponse() {
        LoginResponse loginResponse = new LoginResponse("new-access", "new-refresh", "Bearer", 900);
        when(authService.refresh(any(RefreshRequest.class), anyString(), anyString(), anyString()))
                .thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.refresh(
                new RefreshRequest("old-refresh"), httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().refreshToken()).isEqualTo("new-refresh");
        assertThat(response.getBody().message()).isEqualTo("Token diperbarui");
    }

    @Test
    void logout_returnsNoContentAndDelegatesPrincipal() {
        JwtPrincipal principal = new JwtPrincipal(
                UUID.randomUUID(), "user@example.id", Set.of("BORROWER"), "jti", Instant.now().plusSeconds(900)
        );

        ResponseEntity<Void> response = authController.logout(principal, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(authService).logout(principal, "127.0.0.1", "JUnit-Agent");
    }

    @Test
    void me_returnsAuthenticatedUserProfile() {
        UserResponse userResponse = userResponse();
        when(authService.getUserById(userResponse.userId())).thenReturn(userResponse);
        JwtPrincipal principal = new JwtPrincipal(
                userResponse.userId(), userResponse.email(), Set.of("BORROWER"), "jti", Instant.now().plusSeconds(900)
        );

        ResponseEntity<ApiResponse<UserResponse>> response = authController.me(principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().email()).isEqualTo(userResponse.email());
        assertThat(response.getBody().message()).isEqualTo("OK");
    }

    private UserResponse userResponse() {
        return new UserResponse(
                UUID.randomUUID(),
                "user@example.id",
                "+6281234567890",
                UserStatus.ACTIVE,
                Set.of("BORROWER"),
                Instant.now()
        );
    }
}
