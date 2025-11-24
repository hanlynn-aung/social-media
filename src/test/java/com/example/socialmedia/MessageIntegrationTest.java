package com.example.socialmedia;

import com.example.socialmedia.model.Message;
import com.example.socialmedia.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
class MessageIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @Autowired
    MessageRepository messageRepository;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    void testMessagePersistence() {
        // Given
        Message message = new Message();
        message.setShopId(1L);
        message.setSenderId(1L);
        message.setContent("Hello MongoDB Test");
        
        // When
        messageRepository.save(message);
        
        // Then
        List<Message> messages = messageRepository.findByShopIdOrderBySentAtAsc(1L);
        assertEquals(1, messages.size());
        assertEquals("Hello MongoDB Test", messages.get(0).getContent());
        assertNotNull(messages.get(0).getId());
    }
}
