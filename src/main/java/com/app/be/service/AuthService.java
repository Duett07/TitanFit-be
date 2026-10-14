package com.app.be.service;

import com.app.be.dto.auth.AuthResponse;
import com.app.be.dto.auth.LoginRequest;
import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.auth.UserResponse;
import com.app.be.security.user.CustomUserDetails;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(CustomUserDetails userDetails);
}
