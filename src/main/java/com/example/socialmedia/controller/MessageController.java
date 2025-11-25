package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.model.Message;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.service.MessageService;
import com.example.socialmedia.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public MessageController(MessageService messageService, AuthorizationHelper authorizationHelper) {
        this.messageService = messageService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping("/shop/{shopId}")
    @RequireUserRole
    public List<Message> getShopMessages(@PathVariable Long shopId) {
        return messageService.getMessagesByShop(shopId);
    }

    @PostMapping("/user/{userId}/shop/{shopId}")
    @RequireUserRole
    public ResponseEntity<?> sendMessage(@PathVariable Long userId, @PathVariable Long shopId, @Valid @RequestBody Message message) {
        // User can only send messages for themselves
        if (!authorizationHelper.canModifyResource(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("You can only send messages for your own account"));
        }
        
        try {
            Message sentMessage = messageService.sendMessage(userId, shopId, message);
            return ResponseEntity.ok(sentMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @RequireUserRole
    public ResponseEntity<?> deleteMessage(@PathVariable String id) {
        try {
            messageService.deleteMessage(id);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Message deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}/hide")
    @RequireUserRole
    public ResponseEntity<?> hideMessage(@PathVariable String id) {
        try {
            Message hidden = messageService.hideMessage(id);
            return ResponseEntity.ok(hidden);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
