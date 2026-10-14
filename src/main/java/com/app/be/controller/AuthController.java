package com.app.be.controller;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.auth.AuthResponse;
import com.app.be.dto.auth.LoginRequest;
import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.auth.UserResponse;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Đăng nhập thành công", authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin tài khoản thành công", authService.getCurrentUser(userDetails)));
    }
}
