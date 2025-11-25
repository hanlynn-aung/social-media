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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Base64;

/**
 * Filter for validating HMAC-signed requests
 * Used for high-security endpoints (optional)
 */
@Component
public class RequestSigningFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestSigningFilter.class);

    @Value("${security.request-signing.enabled:false}")
    private boolean enabled;

    @Value("${security.request-signing.secret:default-secret-key}")
    private String signingSecret;

    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final long MAX_TIMESTAMP_DIFF = 5 * 60 * 1000; // 5 minutes

    private static final String[] PROTECTED_PATHS = {
            "/api/users/delete",
            "/api/shops/delete",
            "/api/admin/",
            "/api/auth/signup"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Check if this path requires signature
        if (!isProtectedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Validate signature
        if (!validateRequestSignature(httpRequest)) {
            logger.warn("Invalid request signature for: {}", path);
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.getWriter().write("{\"error\":\"Invalid request signature\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isProtectedPath(String path) {
        for (String protectedPath : PROTECTED_PATHS) {
            if (path.startsWith(protectedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean validateRequestSignature(HttpServletRequest request) {
        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);

        if (signature == null || timestamp == null) {
            return false;
        }

        // Validate timestamp
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - requestTimestamp) > MAX_TIMESTAMP_DIFF) {
                logger.warn("Request timestamp too old or in future");
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // Validate signature
        String expectedSignature = generateSignature(
                request.getMethod(),
                request.getRequestURI(),
                timestamp
        );

        return expectedSignature.equals(signature);
    }

    /**
     * Generate HMAC-SHA256 signature for request
     */
    private String generateSignature(String method, String path, String timestamp) {
        try {
            String data = method + "|" + path + "|" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(signingSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] signature = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            logger.error("Error generating signature", e);
            return "";
        }
    }

    /**
     * Public method to generate signature for client use
     */
    public static String generateClientSignature(String method, String path, String timestamp, String secret) {
        try {
            String data = method + "|" + path + "|" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] signature = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            logger.error("Error generating client signature", e);
            return "";
        }
    }
}
