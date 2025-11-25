package com.example.socialmedia.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Filter for rate limiting based on user identity and role
 */
@Component
public class RateLimitingFilter implements Filter {

    @Autowired
    private RateLimitingConfig rateLimitingConfig;

    private static final String[] EXCLUDED_PATHS = {
            "/api/auth/signin",
            "/api/auth/signup",
            "/h2-console",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // Skip rate limiting for excluded paths
        if (isExcludedPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // Get current user/role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous:" + getClientIP(httpRequest);
        String role = "ANONYMOUS";

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            userId = String.valueOf(userDetails.getId());
            role = userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.joining(","));
        }

        // Check rate limit
        boolean allowed = rateLimitingConfig.allowRequest(userId, role);

        if (!allowed) {
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded. Maximum " +
                    getRateLimitForRole(role) + " requests per minute.\"}");
            return;
        }

        // Add rate limit headers
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(getRateLimitForRole(role)));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(
                rateLimitingConfig.getRemainingTokens(userId, role)));

        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.contains(excluded)) {
                return true;
            }
        }
        return false;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        return request.getRemoteAddr();
    }

    private int getRateLimitForRole(String role) {
        return switch (role) {
            case "ADMIN" -> 500;
            case "SHOP_ADMIN" -> 100;
            case "USER" -> 60;
            default -> 10;
        };
    }
}
