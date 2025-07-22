package com.coubee.domain;

import com.coubee.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity for order information
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "order_token")
    private String orderToken;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @Builder
    private Order(String orderId, Long userId, Long storeId, OrderStatus status, Integer totalAmount, String recipientName, String orderToken) {
        this.orderId = orderId;
        this.userId = userId;
        this.storeId = storeId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.recipientName = recipientName;
        this.orderToken = orderToken;
    }

    /**
     * Factory method for creating an order
     */
    public static Order createOrder(String orderId, Long userId, Long storeId, Integer totalAmount, String recipientName) {
        return Order.builder()
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .recipientName(recipientName)
                .build();
    }

    /**
     * Add an item to the order
     */
    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Update order status
     */
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * Set order token
     */
    public void setOrderToken(String orderToken) {
        this.orderToken = orderToken;
    }

    /**
     * Set payment information
     */
    public void setPayment(Payment payment) {
        this.payment = payment;
    }
} 
