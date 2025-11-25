package com.example.socialmedia.audit;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.ZonedDateTime;

/**
 * Audit log entry for sensitive operations
 * Stores in MongoDB for long-term retention
 */
@Document(collection = "audit_logs")
public class AuditLog {

    @org.springframework.data.annotation.Id
    private String id;

    @Field
    private Long userId;

    @Field
    private String username;

    @Field
    private String action;

    @Field
    private String resource;

    @Field
    private String resourceId;

    @Field
    private String ipAddress;

    @Field
    private String userAgent;

    @Field
    private String status; // SUCCESS, FAILURE, DENIED

    @Field
    private String details;

    @CreatedDate
    @Field
    private ZonedDateTime timestamp;

    // Constructors
    public AuditLog() {
    }

    public AuditLog(Long userId, String username, String action, String resource, String resourceId,
                    String ipAddress, String userAgent, String status, String details) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = status;
        this.details = details;
        this.timestamp = ZonedDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
