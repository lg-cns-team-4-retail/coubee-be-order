package com.coubee.coubeebeorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "original_amount", nullable = false)
    private Integer originalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;



    @Column(name = "paid_at_unix")
    private Long paidAtUnix;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderTimestamp> statusHistory = new ArrayList<>();

    @Builder
    private Order(String orderId, Long userId, Long storeId, OrderStatus status, Integer originalAmount, Integer discountAmount, Integer totalAmount, String recipientName, Long paidAtUnix) {
        this.orderId = orderId;
        this.userId = userId;
        this.storeId = storeId;
        this.status = status;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount != null ? discountAmount : 0;
        this.totalAmount = totalAmount;
        this.recipientName = recipientName;
        this.paidAtUnix = paidAtUnix;
    }

    public static Order createOrder(String orderId, Long userId, Long storeId, Integer originalAmount, Integer discountAmount, Integer totalAmount, String recipientName) {
        return Order.builder()
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .status(OrderStatus.PENDING)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .recipientName(recipientName)
                .build();
    }

    // Backward compatibility method for existing code
    public static Order createOrder(String orderId, Long userId, Long storeId, Integer totalAmount, String recipientName) {
        return createOrder(orderId, userId, storeId, totalAmount, 0, totalAmount, recipientName);
    }

    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void addStatusHistory(OrderTimestamp timestamp) {
        statusHistory.add(timestamp);
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    /**
     * 결제 완료 시점을 UNIX 타임스탬프로 설정합니다.
     * QR 코드 스캔으로 주문이 결제 완료된 시점을 기록합니다.
     *
     * @param paidAtUnix 결제 완료 시점의 UNIX 타임스탬프
     */
    public void setPaidAtUnix(Long paidAtUnix) {
        this.paidAtUnix = paidAtUnix;
    }

    /**
     * 현재 시점을 결제 완료 시점으로 설정합니다.
     */
    public void markAsPaidNow() {
        this.paidAtUnix = System.currentTimeMillis() / 1000L;
    }
}