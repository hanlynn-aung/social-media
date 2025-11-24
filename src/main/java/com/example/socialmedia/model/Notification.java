package com.example.socialmedia.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "posts", "shops"})
    private User user;

    private String message;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NotificationType {
        SHOP_ANNOUNCEMENT,
        SYSTEM_UPDATE,
        UPCOMING_EVENT
    }
}
