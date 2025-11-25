package com.example.socialmedia.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id; // MongoDB uses String IDs by default

    private Long shopId; // Store Reference ID
    private Long senderId; // Store Reference ID
    private String senderUsername; // Optional: cache username to avoid lookups

    @NotBlank
    private String content;
    
    private MessageStatus status = MessageStatus.VISIBLE;

    private LocalDateTime sentAt = LocalDateTime.now();
    
    public enum MessageStatus {
        VISIBLE,
        HIDDEN
    }
}
