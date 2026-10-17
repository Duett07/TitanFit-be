package com.app.be.security.jwt;

import java.util.UUID;

public interface TokenRedisService {

    void saveRefreshToken(UUID userId, String refreshToken, long expirationSeconds);

    String getRefreshToken(UUID userId);

    void deleteRefreshToken(UUID userId);

    boolean validateRefreshToken(UUID userId, String refreshToken);

    void blacklistAccessToken(String accessToken, long remainingTtlSeconds);

    boolean isBlacklisted(String accessToken);
}
