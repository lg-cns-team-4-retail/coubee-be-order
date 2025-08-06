package com.coubee.coubeebeorder.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

    private String eventId;
    private String eventType;
    private String orderId;
    private Long userId;
    private String orderStatus;
    private Long totalAmount;
    private LocalDateTime timestamp;
    private List<OrderItemEvent> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent {
        private Long productId; // 타입을 Long으로 수정
        private Integer quantity;
    }

    public static OrderEvent createOrderCreated(String orderId, Long userId, Long totalAmount) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CREATED")
                .orderId(orderId)
                .userId(userId)
                .orderStatus("PENDING")
                .totalAmount(totalAmount)
                .timestamp(LocalDateTime.now())
                .items(Collections.emptyList()) // 비어있는 리스트 전달
                .build();
    }

    public static OrderEvent createOrderCompleted(String orderId, Long userId, Long totalAmount) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_COMPLETED")
                .orderId(orderId)
                .userId(userId)
                .orderStatus("COMPLETED")
                .totalAmount(totalAmount)
                .timestamp(LocalDateTime.now())
                .items(Collections.emptyList()) // 비어있는 리스트 전달
                .build();
    }

    public static OrderEvent createOrderCancelled(String orderId, Long userId, Long totalAmount) {
        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ORDER_CANCELLED")
                .orderId(orderId)
                .userId(userId)
                .orderStatus("CANCELLED")
                .totalAmount(totalAmount)
                .timestamp(LocalDateTime.now())
                .items(Collections.emptyList()) // 비어있는 리스트 전달
                .build();
    }
}