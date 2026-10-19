package com.app.be.config;

import com.app.be.dto.ApiResponse;
import com.app.be.security.jwt.JwtAccessDeniedHandler;
import com.app.be.security.jwt.JwtAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    @DisplayName("JacksonConfig: Bean ObjectMapper duoc khoi tao hop le va ho tro Java 8 date/time")
    void objectMapper_ConfiguredCorrectly() throws Exception {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper).isNotNull();

        // Verify JavaTimeModule serializes without timestamp numbers
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0, 0);
        String json = mapper.writeValueAsString(now);
        assertThat(json).contains("2026-10-19T12:00:00");
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint: Hoat dong dung khi ObjectMapper duoc inject hoac fallback")
    void jwtAuthenticationEntryPoint_FallbackAndInjected() throws Exception {
        // Fallback test with null mapper
        JwtAuthenticationEntryPoint entryPointFallback = new JwtAuthenticationEntryPoint(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPointFallback.commence(request, response, new BadCredentialsException("Unauthorized"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Yêu cầu xác thực tài khoản");

        // Test with configured ObjectMapper
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        JwtAuthenticationEntryPoint entryPointConfigured = new JwtAuthenticationEntryPoint(mapper);
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        entryPointConfigured.commence(request, response2, new BadCredentialsException("Unauthorized"));
        assertThat(response2.getStatus()).isEqualTo(401);
        assertThat(response2.getContentAsString()).contains("Yêu cầu xác thực tài khoản");
    }

    @Test
    @DisplayName("JwtAccessDeniedHandler: Hoat dong dung khi ObjectMapper duoc inject hoac fallback")
    void jwtAccessDeniedHandler_FallbackAndInjected() throws Exception {
        // Fallback test with null mapper
        JwtAccessDeniedHandler handlerFallback = new JwtAccessDeniedHandler(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlerFallback.handle(request, response, new AccessDeniedException("Forbidden"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Bạn không có quyền thực hiện hành động này");

        // Test with configured ObjectMapper
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        JwtAccessDeniedHandler handlerConfigured = new JwtAccessDeniedHandler(mapper);
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        handlerConfigured.handle(request, response2, new AccessDeniedException("Forbidden"));
        assertThat(response2.getStatus()).isEqualTo(403);
        assertThat(response2.getContentAsString()).contains("Bạn không có quyền thực hiện hành động này");
    }
}
