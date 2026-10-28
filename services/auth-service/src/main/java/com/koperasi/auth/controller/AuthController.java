package com.koperasi.auth.controller;

import com.koperasi.auth.domain.dto.ApiResponse;
import com.koperasi.auth.domain.dto.ForgotPasswordRequest;
import com.koperasi.auth.domain.dto.LoginRequest;
import com.koperasi.auth.domain.dto.LoginResponse;
import com.koperasi.auth.domain.dto.RefreshRequest;
import com.koperasi.auth.domain.dto.RegisterRequest;
import com.koperasi.auth.domain.dto.ResetPasswordRequest;
import com.koperasi.auth.domain.dto.UserResponse;
import com.koperasi.auth.security.JwtPrincipal;
import com.koperasi.auth.service.AuthService;
import com.koperasi.auth.service.PasswordResetService;
import com.koperasi.auth.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        UserResponse response = authService.register(
                request,
                RequestUtils.deviceInfo(httpRequest),
                RequestUtils.clientIp(httpRequest),
                RequestUtils.userAgent(httpRequest)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Registrasi berhasil, silakan verifikasi akun"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(
                request,
                RequestUtils.deviceInfo(httpRequest),
                RequestUtils.clientIp(httpRequest),
                RequestUtils.userAgent(httpRequest)
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Login berhasil"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request,
                                                              HttpServletRequest httpRequest) {
        LoginResponse response = authService.refresh(
                request,
                RequestUtils.deviceInfo(httpRequest),
                RequestUtils.clientIp(httpRequest),
                RequestUtils.userAgent(httpRequest)
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Token diperbarui"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal JwtPrincipal principal,
                                       HttpServletRequest httpRequest) {
        authService.logout(principal, RequestUtils.clientIp(httpRequest), RequestUtils.userAgent(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                            HttpServletRequest httpRequest) {
        passwordResetService.requestReset(
                request.email(),
                RequestUtils.clientIp(httpRequest),
                RequestUtils.userAgent(httpRequest)
        );
        return ResponseEntity.accepted().body(ApiResponse.ok(null,
                "Jika email terdaftar, tautan reset kata sandi telah dikirim"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                           HttpServletRequest httpRequest) {
        passwordResetService.reset(
                request.token(),
                request.newPassword(),
                RequestUtils.clientIp(httpRequest),
                RequestUtils.userAgent(httpRequest)
        );
        return ResponseEntity.ok(ApiResponse.ok(null,
                "Kata sandi berhasil diperbarui, silakan masuk kembali"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal JwtPrincipal principal) {
        UserResponse response = authService.getUserById(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(response, "OK"));
    }
}
