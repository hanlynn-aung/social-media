package com.example.socialmedia.audit;

import com.example.socialmedia.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Aspect for audit logging sensitive operations
 */
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log user create/update/delete operations
     */
    @Around("execution(* com.example.socialmedia.controller.UserController.*(..)) && " +
            "!execution(* *.get*(..))")
    public Object auditUserOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "USER_MANAGEMENT");
    }

    /**
     * Log shop operations
     */
    @Around("execution(* com.example.socialmedia.controller.ShopController.*(..)) && " +
            "!execution(* *.get*(..))")
    public Object auditShopOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "SHOP_MANAGEMENT");
    }

    /**
     * Log admin operations
     */
    @Around("execution(* com.example.socialmedia.controller.*Controller.*(..)) && " +
            "execution(* *.delete*(..))")
    public Object auditDeleteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "DELETE");
    }

    /**
     * Log access denied events
     */
    @AfterThrowing(pointcut = "@annotation(org.springframework.security.access.prepost.PreAuthorize)", 
                   throwing = "exception")
    public void auditAccessDenied(JoinPoint joinPoint, Exception exception) {
        String userId = getCurrentUserId();
        String username = getCurrentUsername();
        String action = joinPoint.getSignature().getName();
        String resource = joinPoint.getTarget().getClass().getSimpleName();

        AuditLog log = new AuditLog(
                userId != null ? Long.valueOf(userId) : null,
                username,
                action,
                resource,
                getResourceId(joinPoint),
                getClientIP(),
                getUserAgent(),
                "DENIED",
                exception.getMessage()
        );

        auditLogRepository.save(log);
    }

    /**
     * Generic audit operation method
     */
    private Object auditOperation(ProceedingJoinPoint joinPoint, String operationType) throws Throwable {
        String userId = getCurrentUserId();
        String username = getCurrentUsername();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String resourceId = getResourceId(joinPoint);

        try {
            Object result = joinPoint.proceed();

            // Log successful operation
            AuditLog log = new AuditLog(
                    userId != null ? Long.valueOf(userId) : null,
                    username,
                    operationType + "_" + methodName.toUpperCase(),
                    className,
                    resourceId,
                    getClientIP(),
                    getUserAgent(),
                    "SUCCESS",
                    "Operation completed successfully"
            );

            auditLogRepository.save(log);

            return result;
        } catch (Exception e) {
            // Log failed operation
            AuditLog log = new AuditLog(
                    userId != null ? Long.valueOf(userId) : null,
                    username,
                    operationType + "_" + methodName.toUpperCase(),
                    className,
                    resourceId,
                    getClientIP(),
                    getUserAgent(),
                    "FAILURE",
                    e.getMessage()
            );

            auditLogRepository.save(log);

            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl) {
            return String.valueOf(((UserDetailsImpl) auth.getPrincipal()).getId());
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "ANONYMOUS";
    }

    private String getResourceId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Number) {
            return args[0].toString();
        }
        return "unknown";
    }

    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
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

    private String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        String userAgent = attributes.getRequest().getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
}
