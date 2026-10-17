package com.app.be.security.ratelimit;

import com.app.be.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RateLimiterService {

    // Login: Tối đa 20 lượt thử / 1 phút theo IP
    private final Map<String, Bucket> loginIpBuckets = new ConcurrentHashMap<>();

    // Login: Tối đa 5 lượt thử / 15 phút theo cặp IP + Email
    private final Map<String, Bucket> loginAccountBuckets = new ConcurrentHashMap<>();

    // Register: Tối đa 5 tài khoản / 1 giờ theo IP
    private final Map<String, Bucket> registerIpBuckets = new ConcurrentHashMap<>();

    /**
     * Kiểm tra hạn mức đăng nhập (Rate limit 2 tầng: IP và IP+Email).
     */
    public void checkLoginRateLimit(String ip, String email) {
        String normalizedIp = normalizeKey(ip);
        String normalizedEmail = normalizeKey(email);

        // 1. Kiểm tra hạn mức theo IP (20 lượt / phút)
        Bucket ipBucket = loginIpBuckets.computeIfAbsent(normalizedIp, k -> createBucket(20, Duration.ofMinutes(1)));
        ConsumptionProbe ipProbe = ipBucket.tryConsumeAndReturnRemaining(1);
        if (!ipProbe.isConsumed()) {
            long retryAfter = calculateRetryAfterSeconds(ipProbe.getNanosToWaitForRefill());
            log.warn("Rate limit exceeded for login from IP {}. Retry after {}s", normalizedIp, retryAfter);
            throw new RateLimitExceededException(
                    "Quá nhiều yêu cầu đăng nhập từ địa chỉ IP của bạn. Vui lòng thử lại sau " + retryAfter + " giây.",
                    retryAfter
            );
        }

        // 2. Kiểm tra hạn mức theo tài khoản và IP (5 lượt / 15 phút)
        String accountKey = normalizedIp + ":" + normalizedEmail;
        Bucket accountBucket = loginAccountBuckets.computeIfAbsent(accountKey, k -> createBucket(5, Duration.ofMinutes(15)));
        ConsumptionProbe accountProbe = accountBucket.tryConsumeAndReturnRemaining(1);
        if (!accountProbe.isConsumed()) {
            long retryAfter = calculateRetryAfterSeconds(accountProbe.getNanosToWaitForRefill());
            log.warn("Rate limit exceeded for account login: {} from IP {}. Retry after {}s", normalizedEmail, normalizedIp, retryAfter);
            throw new RateLimitExceededException(
                    "Bạn đã thử đăng nhập sai quá nhiều lần cho tài khoản này. Vui lòng thử lại sau " + retryAfter + " giây.",
                    retryAfter
            );
        }
    }

    /**
     * Đặt lại (reset) số lần thử đăng nhập khi tài khoản đăng nhập thành công.
     */
    public void resetLoginLimit(String ip, String email) {
        String accountKey = normalizeKey(ip) + ":" + normalizeKey(email);
        Bucket accountBucket = loginAccountBuckets.get(accountKey);
        if (accountBucket != null) {
            accountBucket.reset();
        }
    }

    /**
     * Kiểm tra hạn mức đăng ký tài khoản mới (5 tài khoản / giờ theo IP).
     */
    public void checkRegisterRateLimit(String ip) {
        String normalizedIp = normalizeKey(ip);
        Bucket bucket = registerIpBuckets.computeIfAbsent(normalizedIp, k -> createBucket(5, Duration.ofHours(1)));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfter = calculateRetryAfterSeconds(probe.getNanosToWaitForRefill());
            long minutes = Math.max(1, (retryAfter + 59) / 60);
            log.warn("Rate limit exceeded for register from IP {}. Retry after {}s", normalizedIp, retryAfter);
            throw new RateLimitExceededException(
                    "Bạn đã tạo quá nhiều tài khoản từ mạng này. Vui lòng thử lại sau " + minutes + " phút.",
                    retryAfter
            );
        }
    }

    private Bucket createBucket(long capacity, Duration duration) {
        Bandwidth limit = Bandwidth.simple(capacity, duration);
        return Bucket.builder().addLimit(limit).build();
    }

    private String normalizeKey(String key) {
        return key != null ? key.trim().toLowerCase() : "unknown";
    }

    private long calculateRetryAfterSeconds(long nanos) {
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
        return Math.max(1, seconds);
    }
}
