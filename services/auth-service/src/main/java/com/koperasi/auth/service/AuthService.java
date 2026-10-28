package com.koperasi.auth.service;

import com.koperasi.auth.domain.dto.LoginRequest;
import com.koperasi.auth.domain.dto.LoginResponse;
import com.koperasi.auth.domain.dto.RefreshRequest;
import com.koperasi.auth.domain.dto.RegisterRequest;
import com.koperasi.auth.domain.dto.UserResponse;
import com.koperasi.auth.security.JwtPrincipal;

import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request, String deviceInfo, String ipAddress, String userAgent);

    LoginResponse login(LoginRequest request, String deviceInfo, String ipAddress, String userAgent);

    LoginResponse refresh(RefreshRequest request, String deviceInfo, String ipAddress, String userAgent);

    void logout(JwtPrincipal principal, String ipAddress, String userAgent);

    UserResponse getUserById(UUID userId);
}
