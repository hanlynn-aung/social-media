package com.example.socialmedia.aspect;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimiterAspect {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = getClientKey();
        // We create a unique key per method + client to ensure limits are per endpoint-user pair, 
        // or just per client globally?
        // Usually rate limits are per endpoint or global. 
        // If I use just 'key' (userId), it's a global limit for that user across all rate-limited endpoints sharing the same config.
        // But if the annotation has different capacities, we need different buckets.
        // So the key should include the method signature or name.
        
        String bucketKey = key + "-" + joinPoint.getSignature().toShortString();
        
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createNewBucket(rateLimit));

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again later.");
        }
    }

    private Bucket createNewBucket(RateLimit rateLimit) {
        Bandwidth limit = Bandwidth.classic(rateLimit.capacity(), 
            Refill.greedy(rateLimit.capacity(), Duration.ofSeconds(rateLimit.duration())));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientKey() {
        // Try to get user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        // Fallback to IP address
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null) {
                return xForwardedFor.split(",")[0];
            }
            return request.getRemoteAddr();
        }
        
        return "anonymous";
    }
}
