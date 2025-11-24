package com.example.socialmedia.controller;

import com.example.socialmedia.model.Notification;
import com.example.socialmedia.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        return notificationService.getUserNotifications(userId);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
    
    // Admin endpoint to broadcast system updates
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcastMessage(@RequestParam String message) {
        notificationService.broadcastNotification(message, Notification.NotificationType.SYSTEM_UPDATE);
        return ResponseEntity.ok().build();
    }
}
