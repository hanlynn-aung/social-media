package com.example.socialmedia.security;

import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class AuthorizationHelper {

    private final UserRepository userRepository;

    public AuthorizationHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the current authenticated user
     */
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userRepository.findById(userDetails.getId());
        }
        return Optional.empty();
    }

    /**
     * Get current user ID
     */
    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl) {
            return Optional.of(((UserDetailsImpl) auth.getPrincipal()).getId());
        }
        return Optional.empty();
    }

    /**
     * Get current user's authorities
     */
    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities();
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return getCurrentAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Check if user is shop admin
     */
    public boolean isShopAdmin() {
        return getCurrentAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SHOP_ADMIN") || auth.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if user owns the resource (user ID matches)
     */
    public boolean isResourceOwner(Long resourceUserId) {
        return getCurrentUserId()
                .map(userId -> userId.equals(resourceUserId))
                .orElse(false);
    }

    /**
     * Check if user can modify resource (owner or admin)
     */
    public boolean canModifyResource(Long resourceUserId) {
        return isResourceOwner(resourceUserId) || isAdmin();
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String);
    }

    /**
     * Get current username
     */
    public Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl) {
            return Optional.of(((UserDetailsImpl) auth.getPrincipal()).getUsername());
        }
        return Optional.empty();
    }
}
