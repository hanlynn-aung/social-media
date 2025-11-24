package com.example.socialmedia.service;

import com.example.socialmedia.model.Notification;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.NotificationRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void createNotificationForUser(User user, String message, Notification.NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        Notification savedNotification = notificationRepository.save(notification);
        
        // Push to WebSocket: /topic/user/{userId}/notifications
        messagingTemplate.convertAndSend("/topic/user/" + user.getId() + "/notifications", savedNotification);
    }

    public void broadcastNotification(String message, Notification.NotificationType type) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            createNotificationForUser(user, message, type);
        }
    }
    
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}
