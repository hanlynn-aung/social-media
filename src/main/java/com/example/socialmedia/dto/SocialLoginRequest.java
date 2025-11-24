package com.example.socialmedia.dto;

import lombok.Data;

@Data
public class SocialLoginRequest {
    private String provider; // "google" or "wechat"
    private String token; // The token from the provider
    private String email; // Extracted on client side or backend
    private String name;
}
