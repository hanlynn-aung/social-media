package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.model.Review;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.service.ReviewService;
import com.example.socialmedia.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public ReviewController(ReviewService reviewService, AuthorizationHelper authorizationHelper) {
        this.reviewService = reviewService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<?> getShopReviews(@PathVariable Long shopId) {
        try {
            List<Review> reviews = reviewService.getReviewsByShop(shopId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}/shop/{shopId}")
    @RequireUserRole
    public ResponseEntity<?> addReview(@PathVariable Long userId, @PathVariable Long shopId, @Valid @RequestBody Review review) {
        // User can only add reviews for themselves
        if (!authorizationHelper.canModifyResource(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("You can only add reviews for your own account"));
        }
        
        try {
            Review addedReview = reviewService.addReview(userId, shopId, review);
            return ResponseEntity.ok(addedReview);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
