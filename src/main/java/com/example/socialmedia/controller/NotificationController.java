package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireAdminRole;
import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.model.Notification;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.service.NotificationService;
import com.example.socialmedia.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public NotificationController(NotificationService notificationService, AuthorizationHelper authorizationHelper) {
        this.notificationService = notificationService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping("/user/{userId}")
    @RequireUserRole
    public ResponseEntity<?> getUserNotifications(@PathVariable Long userId) {
        // Users can only view their own notifications
        if (!authorizationHelper.canModifyResource(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("You can only view your own notifications"));
        }
        
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    @RequireUserRole
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    // Admin endpoint to broadcast system updates
    @PostMapping("/broadcast")
    @RequireAdminRole
    public ResponseEntity<?> broadcastMessage(@RequestParam String message) {
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("Message cannot be empty"));
        }
        
        try {
            notificationService.broadcastNotification(message, Notification.NotificationType.SYSTEM_UPDATE);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Broadcast notification sent"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
