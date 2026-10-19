package com.app.be.security.jwt;

import com.app.be.security.user.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final TokenRedisService tokenRedisService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null) {
                // Parse và xác thực chữ ký token 1 lần duy nhất để tối ưu hiệu năng
                Claims claims = jwtUtils.parseAndValidateClaims(jwt);
                if (claims != null) {
                    // 1. Kiểm tra token có nằm trong blacklist (do logout) không
                    if (tokenRedisService.isBlacklisted(jwt)) {
                        log.warn("Rejected blacklisted JWT token on [{}]", request.getServletPath());
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 2. Đảm bảo đây là Access Token, không được dùng Refresh Token để gọi API
                    String tokenType = claims.get("type", String.class);
                    if (tokenType != null && !"ACCESS".equalsIgnoreCase(tokenType)) {
                        log.warn("Rejected non-access JWT token type [{}] on protected endpoint [{}]", tokenType, request.getServletPath());
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 3. Trích xuất email từ subject của claims đã parse
                    String email = claims.getSubject();

                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not set user authentication in security context: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trích xuất Access Token theo cơ chế Dual-Auth:
     * 1. Ưu tiên đọc từ Header Authorization: Bearer <token> (Dành cho Mobile, Postman, hoặc SPA gửi qua header).
     * 2. Fallback đọc từ HttpOnly Cookie "accessToken" (Dành cho trình duyệt web, Next.js/React gửi kèm credentials).
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
