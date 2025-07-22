package com.coubee.domain;

import com.coubee.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity for payment information
 */
@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @Column(name = "pg_provider")
    private String pgProvider;

    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "receipt_url")
    private String receiptUrl;

    // 스토어 ID (추가됨)
    @Column(name = "store_id")
    private Long storeId;

    @Builder
    private Payment(String paymentId, Order order, String pgProvider, String pgTransactionId, String method, Integer amount, 
                   PaymentStatus status, LocalDateTime paidAt, LocalDateTime failedAt, String failReason, 
                   LocalDateTime canceledAt, String cancelReason, String receiptUrl, Long storeId) {
        this.paymentId = paymentId;
        this.order = order;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.failedAt = failedAt;
        this.failReason = failReason;
        this.canceledAt = canceledAt;
        this.cancelReason = cancelReason;
        this.receiptUrl = receiptUrl;
        this.storeId = storeId;
    }

    /**
     * Factory method for creating a payment - creates payment in ready state
     */
    public static Payment createPayment(String paymentId, Order order, String method, Integer amount) {
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .order(order)
                .method(method)
                .amount(amount)
                .status(PaymentStatus.READY)
                .build();
        
        order.setPayment(payment);
        return payment;
    }

    /**
     * Process successful payment
     */
    public void completePayment(String pgProvider, String pgTransactionId, LocalDateTime paidAt, String receiptUrl) {
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.paidAt = paidAt;
        this.receiptUrl = receiptUrl;
        this.status = PaymentStatus.PAID;
        
        if (this.order != null) {
            this.order.updateStatus(OrderStatus.PAID);
        }
    }

    /**
     * Process failed payment
     */
    public void failPayment(LocalDateTime failedAt, String failReason) {
        this.failedAt = failedAt;
        this.failReason = failReason;
        this.status = PaymentStatus.FAILED;
        
        if (this.order != null) {
            this.order.updateStatus(OrderStatus.FAILED);
        }
    }

    /**
     * Process cancelled payment
     */
    public void cancelPayment(LocalDateTime canceledAt, String cancelReason) {
        this.canceledAt = canceledAt;
        this.cancelReason = cancelReason;
        this.status = PaymentStatus.CANCELLED;
        
        if (this.order != null) {
            this.order.updateStatus(OrderStatus.CANCELLED);
        }
    }

    /**
     * Update payment status
     */
    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }
    
    /**
     * Get order items for the payment
     */
    public List<OrderItem> getItems() {
        if (this.order != null) {
            return this.order.getItems();
        }
        return List.of();
    }
    
    /**
     * Get store ID
     */
    public Long getStoreId() {
        return this.storeId;
    }
} 
