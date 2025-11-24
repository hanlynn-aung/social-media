package com.example.socialmedia.controller;

import com.example.socialmedia.model.Shop;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Autowired
    public ShopController(ShopRepository shopRepository, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shop> getShopById(@PathVariable Long id) {
        return shopRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public List<Shop> searchShops(@RequestParam String name) {
        return shopRepository.findByNameContainingIgnoreCase(name);
    }
    
    @GetMapping("/nearby")
    public List<Shop> getNearbyShops(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radius) { // Default radius 10km
        return shopRepository.findShopsNearby(lat, lng, radius);
    }

    @PostMapping("/user/{userId}")
    public Shop createShop(@PathVariable Long userId, @Valid @RequestBody Shop shop) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Ideally verify user role here
        if (owner.getRole() != User.Role.SHOP_ADMIN && owner.getRole() != User.Role.ADMIN) {
             // Just a simple check, in real app use Spring Security
             // owner.setRole(User.Role.SHOP_ADMIN); // Auto-promote?
        }
        
        shop.setOwner(owner);
        return shopRepository.save(shop);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shop> updateShop(@PathVariable Long id, @Valid @RequestBody Shop shopDetails) {
        return shopRepository.findById(id)
                .map(shop -> {
                    shop.setName(shopDetails.getName());
                    shop.setDescription(shopDetails.getDescription());
                    shop.setAddress(shopDetails.getAddress());
                    shop.setLatitude(shopDetails.getLatitude());
                    shop.setLongitude(shopDetails.getLongitude());
                    return ResponseEntity.ok(shopRepository.save(shop));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShop(@PathVariable Long id) {
        shopRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
