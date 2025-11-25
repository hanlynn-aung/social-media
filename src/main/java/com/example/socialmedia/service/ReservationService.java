package com.example.socialmedia.service;

import com.example.socialmedia.dto.PaymentRequest;
import com.example.socialmedia.exception.BadRequestException;
import com.example.socialmedia.exception.ResourceNotFoundException;
import com.example.socialmedia.model.*;
import com.example.socialmedia.repository.PaymentLogRepository;
import com.example.socialmedia.repository.ReservationRepository;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final NotificationService notificationService;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository, 
                              ShopRepository shopRepository, PaymentLogRepository paymentLogRepository,
                              NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.paymentLogRepository = paymentLogRepository;
        this.notificationService = notificationService;
    }

    public List<Reservation> getReservationsByUser(Long userId) {
        return reservationRepository.findByUserId(userId);
    }
    
    public List<Reservation> getReservationsByShop(Long shopId) {
        return reservationRepository.findByShopId(shopId);
    }

    @Transactional
    public Reservation createReservation(Long userId, Long shopId, Reservation reservation) {
        log.info("Creating reservation for user {} at shop {}", userId, shopId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        if (reservation.getReservationTime() == null) {
            throw new BadRequestException("Reservation time is required");
        }

        if (reservation.getReservationTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reservation time cannot be in the past");
        }

        reservation.setUser(user);
        reservation.setShop(shop);
        // By default, status is PENDING and payment is UNPAID
        Reservation savedReservation = reservationRepository.save(reservation);

        // Notify Shop Owner
        if (shop.getOwner() != null) {
            notificationService.createNotificationForUser(shop.getOwner(), 
                    "New reservation at " + shop.getName() + " by " + user.getUsername(), 
                    Notification.NotificationType.SHOP_ANNOUNCEMENT);
        }
        
        log.info("Reservation created with id: {}", savedReservation.getId());
        return savedReservation;
    }
    
    @Transactional
    public Reservation updateStatus(Long id, Reservation.ReservationStatus status) {
        log.info("Updating reservation {} status to {}", id, status);
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        
        Reservation.ReservationStatus oldStatus = reservation.getStatus();
        reservation.setStatus(status);
        Reservation savedReservation = reservationRepository.save(reservation);
        
        // Notify User
        notificationService.createNotificationForUser(reservation.getUser(),
                "Your reservation at " + reservation.getShop().getName() + " has been " + status,
                Notification.NotificationType.SYSTEM_UPDATE);
                
        log.info("Reservation {} status updated from {} to {}", id, oldStatus, status);
        return savedReservation;
    }
    
    @Transactional
    public Reservation processPayment(Long id, PaymentRequest paymentRequest) {
        log.info("Processing payment for reservation {}: {}", id, paymentRequest.getTransactionId());
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        
        // Save Payment Log
        PaymentLog logEntry = PaymentLog.builder()
                .reservationId(id)
                .transactionId(paymentRequest.getTransactionId())
                .amount(paymentRequest.getAmount())
                .paymentMethod(paymentRequest.getPaymentMethod())
                .requestPayload(paymentRequest.getRequestPayload())
                .responsePayload(paymentRequest.getResponsePayload())
                .status(paymentRequest.getStatus())
                .build();
        
        paymentLogRepository.save(logEntry);
        
        // Update Reservation Status if success
        if ("SUCCESS".equalsIgnoreCase(paymentRequest.getStatus())) {
            reservation.setPaymentStatus(Reservation.PaymentStatus.PAID);
            // Auto confirm?
            // reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
            log.info("Payment successful for reservation {}", id);
        } else {
            // Maybe set to failed?
            log.warn("Payment failed for reservation {}: {}", id, paymentRequest.getStatus());
        }

        return reservationRepository.save(reservation);
    }
}
