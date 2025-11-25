package com.example.socialmedia.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * IP whitelisting filter for admin endpoints
 */
@Component
public class IPWhitelistFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(IPWhitelistFilter.class);

    @Value("${security.ip-whitelist.enabled:false}")
    private boolean enabled;

    @Value("${security.ip-whitelist.ips:127.0.0.1,localhost}")
    private String whitelistedIPs;

    private Set<String> whitelist;

    @Value("${security.ip-whitelist.paths:/api/admin/,/api/users/}")
    private String protectedPaths;

    @Autowired
    public IPWhitelistFilter() {
        // Initialize whitelist from config
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        // Initialize whitelist on first use
        if (whitelist == null) {
            initializeWhitelist();
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Check if this path requires IP whitelisting
        if (!isProtectedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Check client IP
        String clientIP = getClientIP(httpRequest);

        if (!isIPWhitelisted(clientIP)) {
            logger.warn("Access denied from non-whitelisted IP: {}", clientIP);
            httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
            httpResponse.getWriter().write("{\"error\":\"Access denied. Your IP is not whitelisted.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private void initializeWhitelist() {
        whitelist = Arrays.stream(whitelistedIPs.split(","))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isProtectedPath(String path) {
        String[] paths = protectedPaths.split(",");
        for (String protectedPath : paths) {
            if (path.startsWith(protectedPath.trim())) {
                return true;
            }
        }
        return false;
    }

    private String getClientIP(HttpServletRequest request) {
        // Check X-Forwarded-For header (for proxied requests)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP.trim();
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    private boolean isIPWhitelisted(String ip) {
        if (whitelist == null) {
            initializeWhitelist();
        }

        // Check exact match
        if (whitelist.contains(ip)) {
            return true;
        }

        // Check localhost variants
        if ("localhost".equals(ip) || "127.0.0.1".equals(ip) || "::1".equals(ip)) {
            return whitelist.stream().anyMatch(w -> w.equalsIgnoreCase("localhost"));
        }

        return false;
    }

    /**
     * Add IP to whitelist
     */
    public void addIPToWhitelist(String ip) {
        if (whitelist == null) {
            initializeWhitelist();
        }
        whitelist.add(ip);
        logger.info("Added IP to whitelist: {}", ip);
    }

    /**
     * Remove IP from whitelist
     */
    public void removeIPFromWhitelist(String ip) {
        if (whitelist != null) {
            whitelist.remove(ip);
            logger.info("Removed IP from whitelist: {}", ip);
        }
    }

    /**
     * Get current whitelist
     */
    public Set<String> getWhitelist() {
        if (whitelist == null) {
            initializeWhitelist();
        }
        return new HashSet<>(whitelist);
    }

    /**
     * Clear and reload whitelist
     */
    public void reloadWhitelist() {
        whitelist = null;
        initializeWhitelist();
        logger.info("Whitelist reloaded");
    }
}
