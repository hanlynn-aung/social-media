package com.example.socialmedia.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username; // Can be username or email
    private String password;
    private String phoneNumber; // For phone login
    private boolean isPhoneLogin;
}
