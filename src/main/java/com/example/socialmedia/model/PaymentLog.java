package com.example.socialmedia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId; // Reference to the reservation

    private String transactionId; // External Transaction ID

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    private String status; // e.g., SUCCESS, FAILED

    private String paymentMethod;
    
    private Double amount;
}
