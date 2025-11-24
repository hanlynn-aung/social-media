package com.example.socialmedia.service;

import com.example.socialmedia.model.Reservation;
import com.example.socialmedia.model.Shop;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.ReservationRepository;
import com.example.socialmedia.repository.ShopRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository, ShopRepository shopRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
    }

    public List<Reservation> getReservationsByUser(Long userId) {
        return reservationRepository.findByUserId(userId);
    }
    
    public List<Reservation> getReservationsByShop(Long shopId) {
        return reservationRepository.findByShopId(shopId);
    }

    public Reservation createReservation(Long userId, Long shopId, Reservation reservation) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        reservation.setUser(user);
        reservation.setShop(shop);
        // By default, status is PENDING and payment is UNPAID
        return reservationRepository.save(reservation);
    }
    
    public Reservation updateStatus(Long id, Reservation.ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reservation.setStatus(status);
        return reservationRepository.save(reservation);
    }
    
    public Reservation updatePaymentStatus(Long id, Reservation.PaymentStatus paymentStatus) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reservation.setPaymentStatus(paymentStatus);
        // Logic: If payment is PAID, maybe auto-confirm reservation?
        if (paymentStatus == Reservation.PaymentStatus.PAID) {
             // Keep it manual or auto? Let's keep manual for now or just leave as is.
        }
        return reservationRepository.save(reservation);
    }
}
