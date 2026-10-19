package com.app.be.service.impl;

import com.app.be.dto.auth.AuthResponse;
import com.app.be.dto.auth.LoginRequest;
import com.app.be.dto.auth.RegisterRequest;
import com.app.be.dto.auth.UserResponse;
import com.app.be.enums.Role;
import com.app.be.entity.User;
import com.app.be.exception.BadRequestException;
import com.app.be.exception.DuplicateResourceException;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.exception.UnauthorizedException;
import com.app.be.repository.UserRepository;
import com.app.be.security.jwt.JwtUtils;
import com.app.be.security.jwt.TokenRedisService;
import com.app.be.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.app.be.security.user.CustomUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final TokenRedisService tokenRedisService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite = "Lax";

    void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }

        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null;
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(phone);
        user.setRole(Role.ROLE_CUSTOMER); // Default role

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return UserResponse.fromEntity(savedUser);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Xung đột trùng lặp dữ liệu người dùng khi đăng ký (race condition): {}", ex.getMessage());
            if (userRepository.existsByEmail(email)) {
                throw new DuplicateResourceException("Email đã được sử dụng");
            }
            if (phone != null && userRepository.existsByPhone(phone)) {
                throw new DuplicateResourceException("Số điện thoại đã được sử dụng");
            }
            throw new DuplicateResourceException("Email hoặc số điện thoại đã được sử dụng");
        }
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        tokenRedisService.saveRefreshToken(user.getId(), refreshToken, jwtUtils.getRefreshTokenExpirationSeconds());

        addTokenCookies(response, accessToken, refreshToken);

        log.info("User logged in successfully: {} (access token: {}s, refresh token: {}s)",
                email, jwtUtils.getAccessTokenExpirationSeconds(), jwtUtils.getRefreshTokenExpirationSeconds());

        return AuthResponse.of(user, accessToken, refreshToken, jwtUtils.getAccessTokenExpirationSeconds());
    }

    @Override
    public AuthResponse refreshToken(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        String tokenToVerify = refreshToken;
        if (!StringUtils.hasText(tokenToVerify) && request != null && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    tokenToVerify = cookie.getValue();
                    break;
                }
            }
        }

        if (!StringUtils.hasText(tokenToVerify)) {
            throw new BadRequestException("Refresh token không được để trống");
        }

        if (!jwtUtils.validateToken(tokenToVerify) || !jwtUtils.isRefreshToken(tokenToVerify)) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String userIdStr = jwtUtils.extractUserId(tokenToVerify);
        if (userIdStr == null) {
            throw new UnauthorizedException("Refresh token không chứa thông tin người dùng hợp lệ");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Định dạng User ID trong token không hợp lệ");
        }

        if (!tokenRedisService.validateRefreshToken(userId, tokenToVerify)) {
            throw new UnauthorizedException("Refresh token đã bị thu hồi hoặc đã được sử dụng. Vui lòng đăng nhập lại.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Refresh Token Rotation: Cấp cặp token mới và lưu Refresh token mới vào Redis
        String newAccessToken = jwtUtils.generateAccessToken(user);
        String newRefreshToken = jwtUtils.generateRefreshToken(user);

        tokenRedisService.saveRefreshToken(user.getId(), newRefreshToken, jwtUtils.getRefreshTokenExpirationSeconds());

        addTokenCookies(response, newAccessToken, newRefreshToken);

        log.info("Refreshed tokens successfully for user: {}", user.getEmail());

        return AuthResponse.of(user, newAccessToken, newRefreshToken, jwtUtils.getAccessTokenExpirationSeconds());
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        if (request != null) {
            String token = parseToken(request);
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                try {
                    Date expiration = jwtUtils.extractExpiration(token);
                    long remainingSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                    if (remainingSeconds > 0) {
                        tokenRedisService.blacklistAccessToken(token, remainingSeconds);
                    }
                    String userIdStr = jwtUtils.extractUserId(token);
                    if (userIdStr != null) {
                        tokenRedisService.deleteRefreshToken(UUID.fromString(userIdStr));
                    }
                } catch (Exception e) {
                    log.warn("Error invalidating token during logout: {}", e.getMessage());
                }
            }
        }
        clearTokenCookies(response);
        log.info("User logged out and tokens invalidated");
    }

    @Override
    public void logout(HttpServletResponse response) {
        logout(null, response);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Người dùng chưa được xác thực");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản người dùng không tồn tại hoặc đã bị khoá/xoá"));

        return UserResponse.fromEntity(user);
    }

    private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtUtils.getAccessTokenExpirationSeconds())
                .sameSite(cookieSameSite)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(jwtUtils.getRefreshTokenExpirationSeconds())
                .sameSite(cookieSameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        // Xoá refreshToken trên cả đường dẫn giới hạn (/api/auth) và đường dẫn gốc (/)
        ResponseCookie clearRefreshApi = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        ResponseCookie clearRefreshRoot = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshApi.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshRoot.toString());
    }

    private String parseToken(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
