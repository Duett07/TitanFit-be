package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.auth.AuthResponse;
import com.app.be.dto.auth.LoginRequest;
import com.app.be.dto.auth.RefreshTokenRequest;
import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.auth.UserResponse;
import com.app.be.security.ratelimit.RateLimiterService;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.AuthService;
import com.app.be.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = ClientIpUtils.getClientIp(httpRequest);
        rateLimiterService.checkRegisterRateLimit(clientIp);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String clientIp = ClientIpUtils.getClientIp(httpRequest);
        rateLimiterService.checkLoginRateLimit(clientIp, request.getEmail());

        AuthResponse authResponse = authService.login(request, response);
        rateLimiterService.resetLoginLimit(clientIp, request.getEmail());

        return ResponseEntity
                .ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String token = (request != null && StringUtils.hasText(request.getRefreshToken()))
                ? request.getRefreshToken().trim()
                : null;
        AuthResponse response = authService.refreshToken(token, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(request, response);
        return ResponseEntity
                .ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin tài khoản thành công", authService.getCurrentUser(userId)));
    }
}
