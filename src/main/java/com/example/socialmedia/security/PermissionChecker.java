package com.example.socialmedia.security;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component for checking fine-grained permissions
 */
@Component
public class PermissionChecker {

    /**
     * Check if user has permission for action on resource
     */
    public boolean hasPermission(String userId, String resource, String action) {
        // This would typically query a database
        // For now, using role-based defaults
        String role = getCurrentUserRole();
        return Permission.isActionAllowed(role, resource, action);
    }

    /**
     * Check if user can read resource
     */
    public boolean canRead(String resource) {
        String role = getCurrentUserRole();
        return Permission.isActionAllowed(role, resource, "READ");
    }

    /**
     * Check if user can create resource
     */
    public boolean canCreate(String resource) {
        String role = getCurrentUserRole();
        return Permission.isActionAllowed(role, resource, "CREATE");
    }

    /**
     * Check if user can update resource
     */
    public boolean canUpdate(String resource) {
        String role = getCurrentUserRole();
        return Permission.isActionAllowed(role, resource, "UPDATE");
    }

    /**
     * Check if user can delete resource
     */
    public boolean canDelete(String resource) {
        String role = getCurrentUserRole();
        return Permission.isActionAllowed(role, resource, "DELETE");
    }

    /**
     * Check if user can share resource
     */
    public boolean canShare(String resource) {
        String role = getCurrentUserRole();
        return Permission.isActionAllowed(role, resource, "SHARE");
    }

    /**
     * Get all permissions for resource
     */
    public Map<String, Boolean> getResourcePermissions(String resource) {
        String role = getCurrentUserRole();
        Map<String, Integer> perms = Permission.getDefaultPermissions(role);

        return perms.entrySet().stream()
                .filter(e -> e.getKey().startsWith(resource + "_"))
                .collect(Collectors.toMap(
                        e -> e.getKey().replace(resource + "_", ""),
                        e -> e.getValue() == 1
                ));
    }

    /**
     * Get current user's role
     */
    private String getCurrentUserRole() {
        org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "ANONYMOUS";
        }

        Collection<?> authorities = auth.getAuthorities();
        if (authorities.isEmpty()) {
            return "ANONYMOUS";
        }

        return authorities.stream()
                .map(Object::toString)
                .map(s -> s.replace("ROLE_", ""))
                .collect(Collectors.joining(","));
    }

    /**
     * Check if user has any of the given permissions
     */
    public boolean hasAnyPermission(String resource, String... actions) {
        for (String action : actions) {
            if (hasPermission(null, resource, action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the given permissions
     */
    public boolean hasAllPermissions(String resource, String... actions) {
        for (String action : actions) {
            if (!hasPermission(null, resource, action)) {
                return false;
            }
        }
        return true;
    }
}
