package com.example.socialmedia.service;

import com.example.socialmedia.model.Review;
import com.example.socialmedia.model.Shop;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.ReviewRepository;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, ShopRepository shopRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }

    public List<Review> getReviewsByShop(Long shopId) {
        return reviewRepository.findByShopId(shopId);
    }

    public Review addReview(Long userId, Long shopId, Review review) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        
        review.setUser(user);
        review.setShop(shop);
        
        Review savedReview = reviewRepository.save(review);
        
        // Update shop rating
        updateShopRating(shop);
        
        return savedReview;
    }
    
    private void updateShopRating(Shop shop) {
        List<Review> reviews = reviewRepository.findByShopId(shop.getId());
        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        shop.setRating(average);
        shopRepository.save(shop);
    }
}
