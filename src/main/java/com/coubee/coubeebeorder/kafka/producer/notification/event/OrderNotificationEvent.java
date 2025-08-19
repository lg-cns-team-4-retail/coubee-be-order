package com.coubee.coubeebeorder.kafka.producer.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 알림 이벤트
 * Notification Service로 전송되어 사용자에게 알림을 발송하는 이벤트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNotificationEvent {

    private String eventId;
    private String notificationType; // ORDER_CREATED, ORDER_COMPLETED, ORDER_CANCELLED
    private String orderId;
    private Long userId;
    private String message;
    private LocalDateTime timestamp;

    /**
     * 주문 생성 알림 이벤트 생성
     */
    public static OrderNotificationEvent createOrderCreated(String orderId, Long userId) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("ORDER_CREATED")
                .orderId(orderId)
                .userId(userId)
                .message("주문이 생성되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 주문 완료 알림 이벤트 생성
     */
    public static OrderNotificationEvent createOrderCompleted(String orderId, Long userId) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("ORDER_COMPLETED")
                .orderId(orderId)
                .userId(userId)
                .message("주문이 완료되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 주문 취소 알림 이벤트 생성
     */
    public static OrderNotificationEvent createOrderCancelled(String orderId, Long userId) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("ORDER_CANCELLED")
                .orderId(orderId)
                .userId(userId)
                .message("주문이 취소되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
