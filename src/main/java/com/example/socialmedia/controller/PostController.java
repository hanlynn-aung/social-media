package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireShopAdminRole;
import com.example.socialmedia.model.Post;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.service.PostService;
import com.example.socialmedia.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public PostController(PostService postService, AuthorizationHelper authorizationHelper) {
        this.postService = postService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping
    public ResponseEntity<?> getAllPosts() {
        try {
            List<Post> posts = postService.getAllPosts();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<?> getPostsByShopId(@PathVariable Long shopId) {
        try {
            List<Post> posts = postService.getPostsByShopId(shopId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/shop/{shopId}")
    @RequireShopAdminRole
    public ResponseEntity<?> createPost(@PathVariable Long shopId, @Valid @RequestBody Post post) {
        try {
            String username = authorizationHelper.getCurrentUsername()
                    .orElseThrow(() -> new IllegalStateException("Current user not found"));
            
            Post created = postService.createPost(shopId, post, username);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequireShopAdminRole
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            postService.deletePost(id);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Post deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
