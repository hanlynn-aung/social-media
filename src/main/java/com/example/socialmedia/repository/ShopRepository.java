package com.example.socialmedia.repository;

import com.example.socialmedia.model.Shop;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByNameContainingIgnoreCase(String name);

    // Haversine formula to find shops within a certain radius (in kilometers)
    @Query(value = "SELECT * FROM shops s WHERE " +
            "(6371 * acos(cos(radians(:userLat)) * cos(radians(s.latitude)) * " +
            "cos(radians(s.longitude) - radians(:userLng)) + " +
            "sin(radians(:userLat)) * sin(radians(s.latitude)))) < :radius", 
            nativeQuery = true)
    List<Shop> findShopsNearby(double userLat, double userLng, double radius);
}
