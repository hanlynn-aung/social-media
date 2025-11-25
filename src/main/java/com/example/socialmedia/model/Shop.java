package com.example.socialmedia.model;

import com.example.socialmedia.util.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Shop name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;
    
    // Basic location storage (Latitude/Longitude could be added for map features)
    private Double latitude;
    private Double longitude;

    private Double rating; // Average rating

    @OneToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"password", "posts", "shops"})
    private User owner;
    
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("shop")
    private List<Post> posts;

    // Custom Builder Pattern implementation
    public static ShopBuilder builder() {
        return new ShopBuilder();
    }

    public static class ShopBuilder implements Builder<Shop> {
        private Long id;
        private String name;
        private String description;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double rating;
        private User owner;
        private List<Post> posts;

        ShopBuilder() {
        }

        public ShopBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ShopBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ShopBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ShopBuilder address(String address) {
            this.address = address;
            return this;
        }

        public ShopBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public ShopBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public ShopBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public ShopBuilder owner(User owner) {
            this.owner = owner;
            return this;
        }

        public ShopBuilder posts(List<Post> posts) {
            this.posts = posts;
            return this;
        }

        @Override
        public Shop build() {
            Shop shop = new Shop();
            shop.setId(this.id);
            shop.setName(this.name);
            shop.setDescription(this.description);
            shop.setAddress(this.address);
            shop.setLatitude(this.latitude);
            shop.setLongitude(this.longitude);
            shop.setRating(this.rating);
            shop.setOwner(this.owner);
            shop.setPosts(this.posts);
            return shop;
        }
    }
}
