package com.example.socialmedia.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Content cannot be empty")
    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id", nullable = false)
    @JsonIgnoreProperties({"posts", "owner"})
    private Shop shop;
    
    // Type of post: GENERAL, EVENT, PROMOTION
    @Enumerated(EnumType.STRING)
    private PostType type = PostType.GENERAL;
    
    public enum PostType {
        GENERAL,
        EVENT,
        PROMOTION
    }

    @CreationTimestamp
    private LocalDateTime createdAt;
}
