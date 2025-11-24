package com.example.socialmedia.service;

import com.example.socialmedia.model.Post;
import com.example.socialmedia.model.Shop;
import com.example.socialmedia.model.User;
import com.example.socialmedia.model.Notification;
import com.example.socialmedia.repository.PostRepository;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;

    @Autowired
    public PostService(PostRepository postRepository, ShopRepository shopRepository, NotificationService notificationService) {
        this.postRepository = postRepository;
        this.shopRepository = shopRepository;
        this.notificationService = notificationService;
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public List<Post> getPostsByShopId(Long shopId) {
        return postRepository.findByShopId(shopId);
    }

    public Post createPost(Long shopId, Post post, String username) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found with id: " + shopId));
        
        // Verify that the user creating the post is the owner of the shop
        if (!shop.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("You are not authorized to post for this shop.");
        }
        
        post.setShop(shop);
        Post savedPost = postRepository.save(post);
        
        // Trigger notification logic
        String message = "New post from " + shop.getName() + ": " + post.getContent();
        
        // Determine notification type
        Notification.NotificationType type = Notification.NotificationType.SHOP_ANNOUNCEMENT;
        if (post.getType() == Post.PostType.EVENT) {
            type = Notification.NotificationType.UPCOMING_EVENT;
            message = "Upcoming Event at " + shop.getName() + ": " + post.getContent();
        }
        
        // For simplicity, we notify ALL users. In a real app, we'd notify followers.
        notificationService.broadcastNotification(message, type);
        
        return savedPost;
    }

    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }
}
