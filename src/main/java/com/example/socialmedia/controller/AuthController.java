package com.example.socialmedia.controller;

import com.example.socialmedia.dto.*;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.security.JwtUtils;
import com.example.socialmedia.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Handle Phone Login specifically if needed, but usually authenticationManager can handle it if customized
        // For simplicity, we assume username/email login here.
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.findByUsername(signUpRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        
        // Set Role
        if (signUpRequest.getRole() != null && signUpRequest.getRole().equalsIgnoreCase("admin")) {
             user.setRole(User.Role.ADMIN);
        } else if (signUpRequest.getRole() != null && signUpRequest.getRole().equalsIgnoreCase("shop_admin")) {
             user.setRole(User.Role.SHOP_ADMIN);
        } else {
             user.setRole(User.Role.USER);
        }

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }
    
    @PostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@Valid @RequestBody SocialLoginRequest socialRequest) {
        // 1. Verify token with provider (Google/WeChat)
        //    This is mocked here. In real app, use Google IdTokenVerifier or WeChat API.
        boolean isValid = true; 
        
        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid Social Token");
        }
        
        // 2. Check if user exists by email (for Google) or OpenID (for WeChat)
        //    We will use email for this example.
        String email = socialRequest.getEmail();
        if (email == null) {
             // Generate a fake one or require user to provide it
             email = socialRequest.getProvider() + "_" + UUID.randomUUID() + "@example.com";
        }
        
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            // Create new user from social info
            user = new User();
            user.setUsername(socialRequest.getName());
            user.setEmail(email);
            user.setPassword(encoder.encode(UUID.randomUUID().toString())); // Random password
            user.setRole(User.Role.USER);
            userRepository.save(user);
        }
        
        // 3. Generate JWT for our app
        //    We need to manually authenticate because we don't know the password
        //    So we assume trust and force authentication
        
        // NOTE: This is a bit hacky for Spring Security standard flow. 
        // Ideally, use a custom UserDetailsService or AuthenticationProvider for social login.
        // Here we will just return the JWT directly generated from User object details.
        
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String jwt = jwtUtils.generateJwtToken(authentication);
        
         List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // Client-side should discard the JWT. 
        // Server-side could blacklist the token if we had Redis or a database table for it.
        return ResponseEntity.ok("Log out successful!");
    }
    
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> resetPasswordRequest(@RequestParam String email) {
        // 1. Check if user exists
        if (userRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Email not found.");
        }
        
        // 2. Generate reset token (uuid)
        // 3. Send email (Mocked)
        String resetToken = UUID.randomUUID().toString();
        System.out.println("Sending password reset email to " + email + " with token: " + resetToken);
        
        return ResponseEntity.ok("Password reset link sent to email!");
    }
}
