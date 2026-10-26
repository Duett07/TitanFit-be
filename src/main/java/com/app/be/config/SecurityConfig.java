package com.app.be.config;

import com.app.be.security.jwt.JwtAccessDeniedHandler;
import com.app.be.security.jwt.JwtAuthenticationEntryPoint;
import com.app.be.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean swaggerEnabled;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint unauthorizedHandler,
            JwtAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API (Protected endpoints authenticate strictly via Bearer header, eliminating CSRF risk)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Custom 401 Unauthorized entry point & 403 Access Denied handler
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Endpoint authorization rules (Strict order: Specific rules first)
                .authorizeHttpRequests(auth -> auth
                        // Customer endpoints - All order endpoints require authentication
                        // Public order tracking by orderCode & verified phone
                        .requestMatchers(HttpMethod.POST, "/api/orders/track").permitAll()

                        // Customer endpoints - All other order endpoints require authentication
                        .requestMatchers("/api/orders", "/api/orders/**").authenticated()

                        // Authenticated auth endpoints (Profile & Security)
                        .requestMatchers("/api/auth/me", "/api/auth/profile", "/api/auth/change-password").authenticated()

                        // Address endpoints
                        .requestMatchers("/api/users/addresses", "/api/users/addresses/**").authenticated()

                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**", "/api/reviews/eligibility/**").permitAll()

                        // Review creation and personal reviews
                        .requestMatchers("/api/reviews", "/api/reviews/**").authenticated()

                        // Swagger / OpenAPI documentation (Công khai ở dev; yêu cầu ROLE_ADMIN khi tắt/production)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).access((authentication, context) -> {
                            if (swaggerEnabled) {
                                return new org.springframework.security.authorization.AuthorizationDecision(true);
                            }
                            // Khi tắt công khai, chỉ cho phép quản trị viên truy cập
                            return new org.springframework.security.authorization.AuthorizationDecision(
                                    authentication.get() != null && authentication.get().getAuthorities().stream()
                                            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                            );
                        })

                        // Error endpoint
                        .requestMatchers("/error").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = (allowedOrigins != null && !allowedOrigins.isEmpty())
                ? allowedOrigins.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
                : List.of("http://localhost:3000", "http://localhost:5173");

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
