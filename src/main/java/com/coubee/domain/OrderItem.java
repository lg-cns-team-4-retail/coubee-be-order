package com.coubee.domain;

import com.coubee.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entity for order item information
 */
@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    @Builder
    private OrderItem(Order order, Long productId, String productName, Integer quantity, Integer price) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Factory method for creating an order item
     */
    public static OrderItem createOrderItem(Order order, Long productId, String productName, Integer quantity, Integer price) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
    }

    /**
     * Set order
     */
    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * Calculate total price of order item
     */
    public Integer getTotalPrice() {
        return price * quantity;
    }
    
    /**
     * Get item ID (alias for product ID for compatibility)
     */
    public Long getItemId() {
        return this.productId;
    }
} 
