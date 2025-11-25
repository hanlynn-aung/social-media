package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireShopAdminRole;
import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.annotation.RequireAdminRole;
import com.example.socialmedia.model.Reservation;
import com.example.socialmedia.security.AuthorizationHelper;
import com.example.socialmedia.service.ReservationService;
import com.example.socialmedia.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthorizationHelper authorizationHelper;

    @Autowired
    public ReservationController(ReservationService reservationService, AuthorizationHelper authorizationHelper) {
        this.reservationService = reservationService;
        this.authorizationHelper = authorizationHelper;
    }

    @GetMapping("/user/{userId}")
    @RequireUserRole
    public ResponseEntity<?> getUserReservations(@PathVariable Long userId) {
        // Users can only view their own reservations
        if (!authorizationHelper.canModifyResource(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("You can only view your own reservations"));
        }
        
        try {
            List<Reservation> reservations = reservationService.getReservationsByUser(userId);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/shop/{shopId}")
    @RequireShopAdminRole
    public ResponseEntity<?> getShopReservations(@PathVariable Long shopId) {
        try {
            List<Reservation> reservations = reservationService.getReservationsByShop(shopId);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}/shop/{shopId}")
    @RequireUserRole
    public ResponseEntity<?> createReservation(@PathVariable Long userId, @PathVariable Long shopId, @RequestBody Reservation reservation) {
        // User can only create reservations for themselves
        if (!authorizationHelper.canModifyResource(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseUtil.buildErrorResponse("You can only create reservations for your own account"));
        }
        
        try {
            Reservation created = reservationService.createReservation(userId, shopId, reservation);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/status")
    @RequireShopAdminRole
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam Reservation.ReservationStatus status) {
        try {
            Reservation updated = reservationService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/payment")
    @RequireUserRole
    public ResponseEntity<?> processPayment(@PathVariable Long id, @RequestBody com.example.socialmedia.dto.PaymentRequest paymentRequest) {
        try {
            Reservation updated = reservationService.processPayment(id, paymentRequest);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse(e.getMessage()));
        }
    }
}
