package com.app.be.security.jwt;

import com.app.be.security.user.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
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
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenRedisService tokenRedisService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Nên xác thực thành công khi request chứa Authorization: Bearer Header")
    void shouldAuthenticateWhenBearerHeaderPresent() throws ServletException, IOException {
        String token = "header-token-123";
        request.addHeader("Authorization", "Bearer " + token);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", "user@titanfit.vn");
        claimsMap.put("type", "ACCESS");
        Claims claims = new DefaultClaims(claimsMap);

        when(jwtUtils.parseAndValidateClaims(token)).thenReturn(claims);
        when(tokenRedisService.isBlacklisted(token)).thenReturn(false);
        when(userDetailsService.loadUserByUsername("user@titanfit.vn")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Nên fallback xác thực bằng Cookie 'accessToken' khi không có Authorization Header")
    void shouldAuthenticateWhenCookieAccessTokenPresentAndHeaderAbsent() throws ServletException, IOException {
        String token = "cookie-token-456";
        request.setCookies(new Cookie("accessToken", token));

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", "cookieuser@titanfit.vn");
        claimsMap.put("type", "ACCESS");
        Claims claims = new DefaultClaims(claimsMap);

        when(jwtUtils.parseAndValidateClaims(token)).thenReturn(claims);
        when(tokenRedisService.isBlacklisted(token)).thenReturn(false);
        when(userDetailsService.loadUserByUsername("cookieuser@titanfit.vn")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Nên ưu tiên Header Authorization hơn Cookie accessToken khi cả hai đều có")
    void shouldPrioritizeHeaderOverCookieWhenBothPresent() throws ServletException, IOException {
        String headerToken = "header-token-priority";
        String cookieToken = "cookie-token-ignored";

        request.addHeader("Authorization", "Bearer " + headerToken);
        request.setCookies(new Cookie("accessToken", cookieToken));

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", "priority@titanfit.vn");
        claimsMap.put("type", "ACCESS");
        Claims claims = new DefaultClaims(claimsMap);

        when(jwtUtils.parseAndValidateClaims(headerToken)).thenReturn(claims);
        when(tokenRedisService.isBlacklisted(headerToken)).thenReturn(false);
        when(userDetailsService.loadUserByUsername("priority@titanfit.vn")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(jwtUtils).parseAndValidateClaims(headerToken);
        verify(jwtUtils, never()).parseAndValidateClaims(cookieToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Không xác thực khi không có cả Header lẫn Cookie")
    void shouldNotAuthenticateWhenBothHeaderAndCookieAbsent() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils, never()).parseAndValidateClaims(any());
        verify(filterChain).doFilter(request, response);
    }
}
