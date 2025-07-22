package com.coubee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Order event class for Kafka messaging
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    
    private String eventId;
    private String eventType;
    private String orderId;
    private Long userId;
    private String orderStatus;
    private Long totalAmount;
    private LocalDateTime timestamp;
    
    public static OrderEvent createOrderCreated(String orderId, Long userId, Long totalAmount) {
        return new OrderEvent(
            java.util.UUID.randomUUID().toString(),
            "ORDER_CREATED",
            orderId,
            userId,
            "PENDING",
            totalAmount,
            LocalDateTime.now()
        );
    }
    
    public static OrderEvent createOrderCompleted(String orderId, Long userId, Long totalAmount) {
        return new OrderEvent(
            java.util.UUID.randomUUID().toString(),
            "ORDER_COMPLETED",
            orderId,
            userId,
            "COMPLETED",
            totalAmount,
            LocalDateTime.now()
        );
    }
    
    public static OrderEvent createOrderCancelled(String orderId, Long userId, Long totalAmount) {
        return new OrderEvent(
            java.util.UUID.randomUUID().toString(),
            "ORDER_CANCELLED",
            orderId,
            userId,
            "CANCELLED",
            totalAmount,
            LocalDateTime.now()
        );
    }
}