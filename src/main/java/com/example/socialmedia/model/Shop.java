package com.example.socialmedia.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop {

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
}
