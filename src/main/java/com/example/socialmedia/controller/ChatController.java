package com.example.socialmedia.controller;

import com.example.socialmedia.dto.ChatMessage;
import com.example.socialmedia.model.Message;
import com.example.socialmedia.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final MessageService messageService;

    @Autowired
    public ChatController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Map messages sent to "/app/chat/{shopId}/sendMessage"
    @MessageMapping("/chat/{shopId}/sendMessage")
    @SendTo("/topic/shop/{shopId}") // Broadcast to subscribers of "/topic/shop/{shopId}"
    public Message sendMessage(@DestinationVariable Long shopId, @Payload ChatMessage chatMessage) {
        
        // Create a new message entity to save to database
        Message message = new Message();
        message.setContent(chatMessage.getContent());
        
        // Save to database (this handles finding the user and shop)
        return messageService.sendMessage(chatMessage.getUserId(), shopId, message);
    }
}
