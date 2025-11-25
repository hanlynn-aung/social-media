package com.example.socialmedia.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Fine-grained permission model for resource-level access control
 */
public class Permission {

    private Long resourceId;
    private String resourceType; // User, Shop, Post, etc.
    private String action; // READ, CREATE, UPDATE, DELETE, SHARE
    private Long userId;
    private String role;

    public enum Action {
        READ("READ"),
        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        SHARE("SHARE"),
        EXECUTE("EXECUTE");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ResourceType {
        USER("USER"),
        SHOP("SHOP"),
        POST("POST"),
        MESSAGE("MESSAGE"),
        RESERVATION("RESERVATION"),
        REVIEW("REVIEW"),
        NOTIFICATION("NOTIFICATION");

        private final String value;

        ResourceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public Permission() {
    }

    public Permission(Long resourceId, String resourceType, String action, Long userId, String role) {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.action = action;
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters
    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Default permissions based on role
     */
    public static Map<String, Integer> getDefaultPermissions(String role) {
        Map<String, Integer> permissions = new HashMap<>();

        switch (role) {
            case "ADMIN":
                // Admin can do everything
                permissions.put("USER_READ", 1);
                permissions.put("USER_CREATE", 1);
                permissions.put("USER_UPDATE", 1);
                permissions.put("USER_DELETE", 1);
                permissions.put("SHOP_READ", 1);
                permissions.put("SHOP_CREATE", 1);
                permissions.put("SHOP_UPDATE", 1);
                permissions.put("SHOP_DELETE", 1);
                permissions.put("POST_READ", 1);
                permissions.put("POST_CREATE", 1);
                permissions.put("POST_UPDATE", 1);
                permissions.put("POST_DELETE", 1);
                break;

            case "SHOP_ADMIN":
                // Shop admin can manage their shop
                permissions.put("SHOP_READ", 1);
                permissions.put("SHOP_UPDATE", 1);
                permissions.put("POST_CREATE", 1);
                permissions.put("POST_UPDATE", 1);
                permissions.put("POST_DELETE", 1);
                permissions.put("RESERVATION_READ", 1);
                permissions.put("RESERVATION_UPDATE", 1);
                break;

            case "USER":
                // Regular user permissions
                permissions.put("USER_READ", 1); // Own profile
                permissions.put("USER_UPDATE", 1); // Own profile
                permissions.put("POST_READ", 1);
                permissions.put("POST_CREATE", 1); // Own posts
                permissions.put("MESSAGE_CREATE", 1);
                permissions.put("MESSAGE_READ", 1);
                permissions.put("RESERVATION_CREATE", 1);
                permissions.put("REVIEW_CREATE", 1);
                break;

            default:
                // Anonymous user - minimal permissions
                permissions.put("USER_READ", 0);
                permissions.put("SHOP_READ", 1);
                permissions.put("POST_READ", 1);
                permissions.put("REVIEW_READ", 1);
        }

        return permissions;
    }

    /**
     * Check if action is allowed on resource
     */
    public static boolean isActionAllowed(String role, String resource, String action) {
        Map<String, Integer> perms = getDefaultPermissions(role);
        String key = resource + "_" + action;
        return perms.getOrDefault(key, 0) == 1;
    }
}
