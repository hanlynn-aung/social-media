package com.example.socialmedia.service;

import com.example.socialmedia.model.Message;
import com.example.socialmedia.model.Shop;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.MessageRepository;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository, UserRepository userRepository, ShopRepository shopRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }

    public List<Message> getMessagesByShop(Long shopId) {
        return messageRepository.findByShopIdOrderBySentAtAsc(shopId);
    }

    public Message sendMessage(Long userId, Long shopId, Message message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        message.setSenderId(user.getId());
        message.setSenderUsername(user.getUsername());
        message.setShopId(shop.getId());
        return messageRepository.save(message);
    }
    
    public void deleteMessage(String messageId) {
        // In a real app, check if the user requesting delete is the shop admin
        messageRepository.deleteById(messageId);
    }
    
    public Message hideMessage(String messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setStatus(Message.MessageStatus.HIDDEN);
        return messageRepository.save(message);
    }
}
