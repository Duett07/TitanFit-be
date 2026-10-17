package com.app.be.security.jwt;

import com.app.be.entity.User;
import com.app.be.security.user.CustomUserDetails;
import com.app.be.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenRedisService tokenRedisService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@titanfit.vn");
        user.setRole("ROLE_CUSTOMER");
        userDetails = CustomUserDetails.build(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate request when Authorization header contains valid Bearer token")
    void doFilterInternal_validBearerHeader_authenticatesUser() throws ServletException, IOException {
        String token = "validAccessToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(tokenRedisService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtils.isAccessToken(token)).thenReturn(true);
        when(jwtUtils.extractEmail(token)).thenReturn("user@titanfit.vn");
        when(userDetailsService.loadUserByUsername("user@titanfit.vn")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@titanfit.vn");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate request when token is only provided via cookie (CSRF protection)")
    void doFilterInternal_cookieOnlyToken_doesNotAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "someAccessTokenFromCookie"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // SecurityContext must NOT be set via cookie
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate when token is blacklisted in Redis")
    void doFilterInternal_blacklistedToken_doesNotAuthenticate() throws ServletException, IOException {
        String token = "revokedToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(tokenRedisService.isBlacklisted(token)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should NOT authenticate when token is a Refresh Token instead of Access Token")
    void doFilterInternal_refreshTokenUsedOnApi_doesNotAuthenticate() throws ServletException, IOException {
        String token = "validRefreshToken";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(tokenRedisService.isBlacklisted(token)).thenReturn(false);
        when(jwtUtils.isAccessToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
