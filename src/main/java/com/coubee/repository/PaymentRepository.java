package com.coubee.repository;

import com.coubee.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Payment entity data access operations
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by payment ID
     *
     * @param paymentId Payment ID
     * @return Payment information Optional
     */
    Optional<Payment> findByPaymentId(String paymentId);

    /**
     * Find payment by order ID
     *
     * @param orderId Order ID
     * @return Payment information Optional
     */
    Optional<Payment> findByOrderOrderId(String orderId);
} 
