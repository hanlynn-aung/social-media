package com.example.socialmedia.controller;

import com.example.socialmedia.model.Review;
import com.example.socialmedia.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/shop/{shopId}")
    public List<Review> getShopReviews(@PathVariable Long shopId) {
        return reviewService.getReviewsByShop(shopId);
    }

    @PostMapping("/user/{userId}/shop/{shopId}")
    public Review addReview(@PathVariable Long userId, @PathVariable Long shopId, @Valid @RequestBody Review review) {
        return reviewService.addReview(userId, shopId, review);
    }
}
