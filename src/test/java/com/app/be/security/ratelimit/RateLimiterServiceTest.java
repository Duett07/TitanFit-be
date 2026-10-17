package com.app.be.security.ratelimit;

import com.app.be.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    @DisplayName("Should block login when account attempts exceed 5 times in 15 minutes")
    void checkLoginRateLimit_exceedsAccountLimit_throwsException() {
        String ip = "192.168.1.100";
        String email = "target@titanfit.vn";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkLoginRateLimit(ip, email);
        }

        assertThatThrownBy(() -> rateLimiterService.checkLoginRateLimit(ip, email))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Bạn đã thử đăng nhập sai quá nhiều lần cho tài khoản này");
    }

    @Test
    @DisplayName("Should block login when IP attempts exceed 20 times in 1 minute")
    void checkLoginRateLimit_exceedsIpLimit_throwsException() {
        String ip = "192.168.1.200";

        // Try different emails from the same IP to test IP-level rate limiting
        for (int i = 0; i < 20; i++) {
            rateLimiterService.checkLoginRateLimit(ip, "user" + i + "@titanfit.vn");
        }

        assertThatThrownBy(() -> rateLimiterService.checkLoginRateLimit(ip, "another@titanfit.vn"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Quá nhiều yêu cầu đăng nhập từ địa chỉ IP của bạn");
    }

    @Test
    @DisplayName("Should reset account login attempts upon successful login")
    void resetLoginLimit_resetsAccountBucket() {
        String ip = "192.168.1.50";
        String email = "user@titanfit.vn";

        // Consume 4 tokens out of 5
        for (int i = 0; i < 4; i++) {
            rateLimiterService.checkLoginRateLimit(ip, email);
        }

        // Reset limit on successful login
        rateLimiterService.resetLoginLimit(ip, email);

        // Can now attempt 5 more times without exception
        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkLoginRateLimit(ip, email);
        }
    }

    @Test
    @DisplayName("Should block register when IP exceeds 5 registrations in an hour")
    void checkRegisterRateLimit_exceedsLimit_throwsException() {
        String ip = "10.0.0.1";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkRegisterRateLimit(ip);
        }

        assertThatThrownBy(() -> rateLimiterService.checkRegisterRateLimit(ip))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Bạn đã tạo quá nhiều tài khoản từ mạng này");
    }
}
