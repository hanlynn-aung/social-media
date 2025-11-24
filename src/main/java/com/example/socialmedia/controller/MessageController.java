package com.example.socialmedia.controller;

import com.example.socialmedia.model.Message;
import com.example.socialmedia.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/shop/{shopId}")
    public List<Message> getShopMessages(@PathVariable Long shopId) {
        return messageService.getMessagesByShop(shopId);
    }

    @PostMapping("/user/{userId}/shop/{shopId}")
    public Message sendMessage(@PathVariable Long userId, @PathVariable Long shopId, @Valid @RequestBody Message message) {
        return messageService.sendMessage(userId, shopId, message);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String id) {
        messageService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/hide")
    public ResponseEntity<Message> hideMessage(@PathVariable String id) {
        return ResponseEntity.ok(messageService.hideMessage(id));
    }
}
