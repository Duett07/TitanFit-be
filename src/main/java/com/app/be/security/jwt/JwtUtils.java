package com.app.be.security.jwt;

import com.app.be.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey signingKey;

    // Default: 30 minutes in seconds (1800s)
    @Value("${jwt.access-token-expiration:1800}")
    private long accessTokenExpirationSeconds;

    // Default: 7 days in seconds (604800s)
    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpirationSeconds;

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    public long getJwtExpiration() {
        return accessTokenExpirationSeconds * 1000;
    }

    /**
     * Generate short-lived Access Token (15-30 minutes).
     */
    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (user.getId() != null) {
            extraClaims.put("userId", user.getId().toString());
        }
        if (user.getRole() != null) {
            extraClaims.put("role", user.getRole());
        }
        if (user.getFullName() != null) {
            extraClaims.put("fullName", user.getFullName());
        }
        extraClaims.put("tokenType", "ACCESS");
        extraClaims.put("jti", UUID.randomUUID().toString());

        return generateToken(extraClaims, user.getEmail(), accessTokenExpirationSeconds * 1000);
    }

    /**
     * Generate long-lived Refresh Token (7 days).
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (user.getId() != null) {
            extraClaims.put("userId", user.getId().toString());
        }
        extraClaims.put("tokenType", "REFRESH");
        extraClaims.put("jti", UUID.randomUUID().toString());

        return generateToken(extraClaims, user.getEmail(), refreshTokenExpirationSeconds * 1000);
    }

    /**
     * Standard token generation delegating to generateAccessToken.
     */
    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    /**
     * Extract token type (ACCESS or REFRESH).
     */
    public String extractTokenType(String token) {
        try {
            return extractClaim(token, claims -> claims.get("tokenType", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        return "REFRESH".equalsIgnoreCase(extractTokenType(token));
    }

    /**
     * Check if token is an access token (or legacy token without explicit type).
     */
    public boolean isAccessToken(String token) {
        String type = extractTokenType(token);
        return type == null || "ACCESS".equalsIgnoreCase(type);
    }

    /**
     * Flexible token generation with custom claims, subject, and expiration.
     */
    public String generateToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract subject (email) from token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract userId claim from token.
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extract role claim from token.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract fullName claim from token.
     */
    public String extractFullName(String token) {
        return extractClaim(token, claims -> claims.get("fullName", String.class));
    }

    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim using a resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and extract all claims from the JWT.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate JWT token structure, signature, and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Validate and initialize the SecretKey on startup. Fail fast if missing, placeholder, invalid Base64, or < 32 bytes.
     */
    @PostConstruct
    public void validateAndInitSigningKey() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret key must not be empty. Please configure 'jwt.secret' (JWT_SECRET) in your environment.");
        }

        String trimmedSecret = jwtSecret.trim();

        if (trimmedSecret.contains("CHANGE_ME")) {
            throw new IllegalStateException("JWT secret key contains placeholder 'CHANGE_ME'. Please generate a secure 256-bit Base64 key before starting the application.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(trimmedSecret);
        } catch (Exception e) {
            throw new IllegalStateException("JWT secret key must be a valid Base64-encoded string. Plain text fallback is not permitted: " + e.getMessage(), e);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 bytes (256 bits) after Base64 decoding. Current key size: " + keyBytes.length + " bytes.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Signing Key successfully initialized with 256-bit+ HMAC-SHA key.");
    }

    /**
     * Returns the cached SecretKey, ensuring initialization if called outside Spring lifecycle.
     */
    private SecretKey getSigningKey() {
        if (this.signingKey == null) {
            validateAndInitSigningKey();
        }
        return this.signingKey;
    }
}
