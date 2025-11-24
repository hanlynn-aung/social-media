package com.example.socialmedia.repository;

import com.example.socialmedia.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByShopIdOrderBySentAtAsc(Long shopId);
}
