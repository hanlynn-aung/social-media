package com.example.socialmedia.controller;

import com.example.socialmedia.model.Reservation;
import com.example.socialmedia.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/user/{userId}")
    public List<Reservation> getUserReservations(@PathVariable Long userId) {
        return reservationService.getReservationsByUser(userId);
    }
    
    @GetMapping("/shop/{shopId}")
    public List<Reservation> getShopReservations(@PathVariable Long shopId) {
        return reservationService.getReservationsByShop(shopId);
    }

    @PostMapping("/user/{userId}/shop/{shopId}")
    public Reservation createReservation(@PathVariable Long userId, @PathVariable Long shopId, @RequestBody Reservation reservation) {
        return reservationService.createReservation(userId, shopId, reservation);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Reservation> updateStatus(@PathVariable Long id, @RequestParam Reservation.ReservationStatus status) {
        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }
    
    @PutMapping("/{id}/payment")
    public ResponseEntity<Reservation> updatePaymentStatus(@PathVariable Long id, @RequestParam Reservation.PaymentStatus status) {
        return ResponseEntity.ok(reservationService.updatePaymentStatus(id, status));
    }
}
