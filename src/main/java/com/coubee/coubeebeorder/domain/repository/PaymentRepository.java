package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);
    
    Optional<Payment> findByOrder_OrderId(String orderId);
}
