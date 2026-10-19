package com.app.be.service;

import com.app.be.dto.auth.AuthResponse;
import com.app.be.dto.auth.ChangePasswordRequest;
import com.app.be.dto.auth.LoginRequest;
import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.auth.UpdateProfileRequest;
import com.app.be.dto.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, HttpServletResponse response);

    AuthResponse refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletResponse response);

    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
