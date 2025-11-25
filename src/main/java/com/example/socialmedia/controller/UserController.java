package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireAdminRole;
import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.annotation.RequireResourceOwner;
import com.example.socialmedia.dto.ChangePasswordRequest;
import com.example.socialmedia.model.User;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.service.UserService;
import com.example.socialmedia.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public UserController(UserService userService, AuthorizationHelper authorizationHelper) {
        this.userService = userService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping
    @RequireAdminRole
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @RequireUserRole
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // Users can view their own profile, admins can view any profile
        if (!authorizationHelper.canModifyResource(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @RequireAdminRole
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    @RequireUserRole
    @RequireResourceOwner("id")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}/change-password")
    @RequireUserRole
    @RequireResourceOwner("id")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(id, request);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequireAdminRole
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
