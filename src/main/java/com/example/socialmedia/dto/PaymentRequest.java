package com.example.socialmedia.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String transactionId;
    private String paymentMethod;
    private Double amount;
    private String requestPayload;
    private String responsePayload;
    private String status; // e.g. "SUCCESS", "FAILED"
}
