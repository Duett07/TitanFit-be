package com.app.be.service.impl;

import com.app.be.dto.auth.AuthResponse;
import com.app.be.dto.auth.LoginRequest;
import com.app.be.entity.User;
import com.app.be.repository.UserRepository;
import com.app.be.security.jwt.JwtUtils;
import com.app.be.security.jwt.TokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenRedisService tokenRedisService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@titanfit.vn");
        testUser.setFullName("Test User");
        testUser.setRole("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should set secure=false and refreshToken path=/api/auth in development")
    void login_developmentCookieConfig_setsCorrectCookies() {
        authService.setCookieSecure(false);
        authService.setCookieSameSite("Lax");

        LoginRequest request = new LoginRequest();
        request.setEmail("test@titanfit.vn");
        request.setPassword("password123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.findByEmail("test@titanfit.vn")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("sampleAccessToken");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("sampleRefreshToken");
        when(jwtUtils.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(jwtUtils.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthResponse authResponse = authService.login(request, response);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.getAccessToken()).isEqualTo("sampleAccessToken");
        assertThat(authResponse.getRefreshToken()).isEqualTo("sampleRefreshToken");

        List<String> cookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeaders).hasSize(2);

        String accessCookie = cookieHeaders.stream()
                .filter(c -> c.startsWith("accessToken="))
                .findFirst()
                .orElseThrow();
        assertThat(accessCookie).contains("Path=/");
        assertThat(accessCookie).contains("HttpOnly");
        assertThat(accessCookie).contains("SameSite=Lax");
        assertThat(accessCookie).doesNotContain("Secure");

        String refreshCookie = cookieHeaders.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElseThrow();
        assertThat(refreshCookie).contains("Path=/api/auth");
        assertThat(refreshCookie).contains("HttpOnly");
        assertThat(refreshCookie).contains("SameSite=Lax");
        assertThat(refreshCookie).doesNotContain("Secure");
    }

    @Test
    @DisplayName("Should set secure=true and SameSite=Lax when production cookie config is active")
    void login_productionCookieConfig_setsSecureFlag() {
        authService.setCookieSecure(true);
        authService.setCookieSameSite("Lax");

        LoginRequest request = new LoginRequest();
        request.setEmail("test@titanfit.vn");
        request.setPassword("password123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.findByEmail("test@titanfit.vn")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("sampleAccessToken");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("sampleRefreshToken");
        when(jwtUtils.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(jwtUtils.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        authService.login(request, response);

        List<String> cookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeaders).hasSize(2);

        for (String cookie : cookieHeaders) {
            assertThat(cookie).contains("Secure");
            assertThat(cookie).contains("HttpOnly");
            assertThat(cookie).contains("SameSite=Lax");
        }
    }

    @Test
    @DisplayName("Logout should clear cookies with Max-Age=0 on both / and /api/auth paths")
    void logout_clearsCookiesWithMatchingPaths() {
        authService.setCookieSecure(true);
        authService.setCookieSameSite("Lax");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer validAccessToken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("validAccessToken")).thenReturn(true);
        when(jwtUtils.extractExpiration("validAccessToken")).thenReturn(new Date(System.currentTimeMillis() + 60000));
        when(jwtUtils.extractUserId("validAccessToken")).thenReturn(userId.toString());

        authService.logout(request, response);

        verify(tokenRedisService).blacklistAccessToken(eq("validAccessToken"), anyLong());
        verify(tokenRedisService).deleteRefreshToken(userId);

        List<String> cookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookieHeaders).hasSize(3);

        assertThat(cookieHeaders).anyMatch(c -> c.startsWith("accessToken=") && c.contains("Max-Age=0") && c.contains("Path=/"));
        assertThat(cookieHeaders).anyMatch(c -> c.startsWith("refreshToken=") && c.contains("Max-Age=0") && c.contains("Path=/api/auth"));
        assertThat(cookieHeaders).anyMatch(c -> c.startsWith("refreshToken=") && c.contains("Max-Age=0") && c.contains("Path=/"));
    }
}
