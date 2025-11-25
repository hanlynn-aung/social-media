package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireShopAdminRole;
import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.annotation.RequireAdminRole;
import com.example.socialmedia.exception.ResourceNotFoundException;
import com.example.socialmedia.model.Shop;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public ShopController(ShopRepository shopRepository, UserRepository userRepository, AuthorizationHelper authorizationHelper) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping
    public ResponseEntity<?> getAllShops() {
        try {
            List<Shop> shops = shopRepository.findAll();
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getShopById(@PathVariable Long id) {
        try {
            return shopRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<?> searchShops(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("Search term cannot be empty"));
        }
        
        try {
            List<Shop> shops = shopRepository.findByNameContainingIgnoreCase(name);
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyShops(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radius) {
        
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("Invalid latitude or longitude"));
        }
        
        if (radius <= 0) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("Radius must be positive"));
        }
        
        try {
            List<Shop> shops = shopRepository.findShopsNearby(lat, lng, radius);
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}")
    @RequireShopAdminRole
    public ResponseEntity<?> createShop(@PathVariable Long userId, @Valid @RequestBody Shop shop) {
        // Shop admin/owner can only create shops for themselves, admin can create for anyone
        if (!authorizationHelper.canModifyResource(userId) && !authorizationHelper.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("You can only create shops for yourself"));
        }
        
        try {
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            
            shop.setOwner(owner);
            Shop created = shopRepository.save(shop);
            return ResponseEntity.ok(created);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequireShopAdminRole
    public ResponseEntity<?> updateShop(@PathVariable Long id, @Valid @RequestBody Shop shopDetails) {
        try {
            return shopRepository.findById(id)
                    .map(shop -> {
                        // Check if user is the owner or admin
                        if (!authorizationHelper.isAdmin() && 
                            (shop.getOwner() == null || !authorizationHelper.canModifyResource(shop.getOwner().getId()))) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .<Object>body(ResponseUtil.buildErrorResponse("You can only update your own shops"));
                        }
                        
                        shop.setName(shopDetails.getName());
                        shop.setDescription(shopDetails.getDescription());
                        shop.setAddress(shopDetails.getAddress());
                        shop.setLatitude(shopDetails.getLatitude());
                        shop.setLongitude(shopDetails.getLongitude());
                        return ResponseEntity.ok(shopRepository.save(shop));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequireAdminRole
    public ResponseEntity<?> deleteShop(@PathVariable Long id) {
        try {
            shopRepository.deleteById(id);
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse("Shop deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
