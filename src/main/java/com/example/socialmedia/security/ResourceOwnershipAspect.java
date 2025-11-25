package com.example.socialmedia.security;

import com.example.socialmedia.annotation.RequireResourceOwner;
import com.example.socialmedia.util.ResponseUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class ResourceOwnershipAspect {

    private final AuthorizationHelper authorizationHelper;

    public ResourceOwnershipAspect(AuthorizationHelper authorizationHelper) {
        this.authorizationHelper = authorizationHelper;
    }

    @Around("@annotation(requireResourceOwner)")
    public Object checkResourceOwnership(ProceedingJoinPoint joinPoint, RequireResourceOwner requireResourceOwner)
            throws Throwable {
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String parameterName = requireResourceOwner.value();
        
        // Find the parameter with @PathVariable matching the required name
        Parameter[] parameters = method.getParameters();
        Object resourceOwnerId = null;
        
        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVar = parameters[i].getAnnotation(PathVariable.class);
            if (pathVar != null) {
                String varName = pathVar.value().isEmpty() ? parameters[i].getName() : pathVar.value();
                if (varName.equals(parameterName)) {
                    resourceOwnerId = joinPoint.getArgs()[i];
                    break;
                }
            }
        }
        
        if (resourceOwnerId == null) {
            throw new IllegalArgumentException("Path variable '" + parameterName + "' not found");
        }
        
        // Check ownership
        Long userId = Long.parseLong(resourceOwnerId.toString());
        if (!authorizationHelper.canModifyResource(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("Access Denied: You can only modify your own resources"));
        }
        
        return joinPoint.proceed();
    }
}
