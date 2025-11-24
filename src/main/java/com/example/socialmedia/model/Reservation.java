package com.example.socialmedia.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "posts", "shops"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id", nullable = false)
    @JsonIgnoreProperties({"posts", "owner"})
    private Shop shop;

    private LocalDateTime reservationTime;
    
    private Integer numberOfGuests;
    
    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum ReservationStatus {
        PENDING,
        CONFIRMED,
        CANCELLED
    }
    
    public enum PaymentStatus {
        UNPAID,
        PAID,
        REFUNDED
    }
}
