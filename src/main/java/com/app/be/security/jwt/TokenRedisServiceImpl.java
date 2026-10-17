package com.app.be.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisServiceImpl implements TokenRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    @Override
    public void saveRefreshToken(UUID userId, String refreshToken, long expirationSeconds) {
        if (userId == null || refreshToken == null) {
            return;
        }
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(expirationSeconds));
            log.debug("Saved refresh token for user {} with TTL {}s", userId, expirationSeconds);
        } catch (Exception e) {
            log.error("Failed to save refresh token in Redis for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public String getRefreshToken(UUID userId) {
        if (userId == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        } catch (Exception e) {
            log.error("Failed to get refresh token from Redis for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteRefreshToken(UUID userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
            log.debug("Deleted refresh token for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete refresh token from Redis for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public boolean validateRefreshToken(UUID userId, String refreshToken) {
        if (userId == null || refreshToken == null) {
            return false;
        }
        String storedToken = getRefreshToken(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    @Override
    public void blacklistAccessToken(String accessToken, long remainingTtlSeconds) {
        if (accessToken == null || remainingTtlSeconds <= 0) {
            return;
        }
        try {
            String key = BLACKLIST_PREFIX + accessToken;
            redisTemplate.opsForValue().set(key, "REVOKED", Duration.ofSeconds(remainingTtlSeconds));
            log.debug("Blacklisted access token for {}s", remainingTtlSeconds);
        } catch (Exception e) {
            log.error("Failed to blacklist access token in Redis: {}", e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        if (accessToken == null) {
            return false;
        }
        try {
            String key = BLACKLIST_PREFIX + accessToken;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check token blacklist in Redis: {}", e.getMessage());
            return false;
        }
    }
}
