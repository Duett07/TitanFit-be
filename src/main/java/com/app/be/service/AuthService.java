package com.app.be.service;

import com.app.be.dto.auth.LoginRequest;
import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.auth.UserResponse;
import com.app.be.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request, HttpServletResponse response);

    UserResponse getCurrentUser(CustomUserDetails userDetails);

    void logout(HttpServletResponse response);
}
