package com.example.socialmedia.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j
 * Supports per-user and per-role rate limiting
 */
@Component
public class RateLimitingConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Rate limits (requests per minute)
    private static final int ANONYMOUS_RATE_LIMIT = 10; // 10 req/min
    private static final int USER_RATE_LIMIT = 60; // 60 req/min
    private static final int SHOP_ADMIN_RATE_LIMIT = 100; // 100 req/min
    private static final int ADMIN_RATE_LIMIT = 500; // 500 req/min

    // API endpoints specific limits
    private static final int UPLOAD_RATE_LIMIT = 5; // 5 uploads/min
    private static final int AUTH_RATE_LIMIT = 5; // 5 auth attempts/min
    private static final int MESSAGE_RATE_LIMIT = 30; // 30 messages/min

    /**
     * Get bucket for a specific user/role
     */
    public Bucket resolveBucket(String key, String role) {
        return buckets.computeIfAbsent(key, k -> createBucket(role));
    }

    /**
     * Get bucket for specific endpoint
     */
    public Bucket resolveEndpointBucket(String key, String endpoint) {
        String bucketKey = key + ":" + endpoint;
        return buckets.computeIfAbsent(bucketKey, k -> createEndpointBucket(endpoint));
    }

    /**
     * Create bucket based on user role
     */
    private Bucket createBucket(String role) {
        int limit = ANONYMOUS_RATE_LIMIT;

        if (role != null) {
            switch (role) {
                case "ADMIN":
                    limit = ADMIN_RATE_LIMIT;
                    break;
                case "SHOP_ADMIN":
                    limit = SHOP_ADMIN_RATE_LIMIT;
                    break;
                case "USER":
                    limit = USER_RATE_LIMIT;
                    break;
                default:
                    limit = ANONYMOUS_RATE_LIMIT;
            }
        }

        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Create bucket for specific endpoint with stricter limits
     */
    private Bucket createEndpointBucket(String endpoint) {
        int limit;

        if (endpoint.contains("/uploads")) {
            limit = UPLOAD_RATE_LIMIT;
        } else if (endpoint.contains("/auth")) {
            limit = AUTH_RATE_LIMIT;
        } else if (endpoint.contains("/messages")) {
            limit = MESSAGE_RATE_LIMIT;
        } else {
            limit = USER_RATE_LIMIT;
        }

        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Check if request is allowed
     */
    public boolean allowRequest(String key, String role) {
        Bucket bucket = resolveBucket(key, role);
        return bucket.tryConsume(1);
    }

    /**
     * Check if endpoint request is allowed
     */
    public boolean allowEndpointRequest(String key, String endpoint) {
        Bucket bucket = resolveEndpointBucket(key, endpoint);
        return bucket.tryConsume(1);
    }

    /**
     * Get remaining tokens for a user
     */
    public long getRemainingTokens(String key, String role) {
        Bucket bucket = resolveBucket(key, role);
        // In Bucket4j, we can check if we can consume tokens
        // If we can, return available tokens, otherwise return 0
        return bucket.tryConsume(1) ? getTokenLimit(role) - 1 : 0;
    }

    private int getTokenLimit(String role) {
        if (role != null) {
            switch (role) {
                case "ADMIN":
                    return ADMIN_RATE_LIMIT;
                case "SHOP_ADMIN":
                    return SHOP_ADMIN_RATE_LIMIT;
                case "USER":
                    return USER_RATE_LIMIT;
                default:
                    return ANONYMOUS_RATE_LIMIT;
            }
        }
        return ANONYMOUS_RATE_LIMIT;
    }

    /**
     * Clear bucket (useful for testing)
     */
    public void clearBucket(String key) {
        buckets.remove(key);
    }

    /**
     * Clear all buckets
     */
    public void clearAllBuckets() {
        buckets.clear();
    }
}
