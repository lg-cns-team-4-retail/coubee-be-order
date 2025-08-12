package com.coubee.coubeebeorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "store_id")
    private Long storeId;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentItem> items;

    @Builder
    private Payment(String paymentId, Order order, String pgProvider, String pgTransactionId, 
                   String method, Integer amount, PaymentStatus status, LocalDateTime paidAt, 
                   String receiptUrl, Long storeId) {
        this.paymentId = paymentId;
        this.order = order;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.receiptUrl = receiptUrl;
        this.storeId = storeId;
    }

    public static Payment createPayment(String paymentId, Order order, String method, Integer amount) {
        return Payment.builder()
                .paymentId(paymentId)
                .order(order)
                .method(method)
                .amount(amount)
                .status(PaymentStatus.READY)
                .storeId(order.getStoreId())
                .build();
    }

    public void updatePaidStatus(String pgProvider, String pgTransactionId, String receiptUrl) {
        this.status = PaymentStatus.PAID;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.receiptUrl = receiptUrl;
        this.paidAt = LocalDateTime.now();
    }

    public void updateFailedStatus() {
        this.status = PaymentStatus.FAILED;
    }

    public void updateCancelledStatus() {
        this.status = PaymentStatus.CANCELLED;
    }

    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }

    public void updatePgTransactionId(String pgTransactionId) {
        this.pgTransactionId = pgTransactionId;
    }

    public void updatePaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}