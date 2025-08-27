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
    private String notificationType; // PAID, CANCELLED_USER, CANCELLED_ADMIN, PREPARING, PREPARED
    private String orderId;
    private Long userId;
    private String title;
    private String message;
    private LocalDateTime timestamp;

    /**
     * 결제 완료 알림 이벤트 생성
     */
    public static OrderNotificationEvent createPaidNotification(String orderId, Long userId, String storeName) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("PAID")
                .orderId(orderId)
                .userId(userId)
                .title("결제 완료")
                .message(storeName + " 매장에서 결제가 완료되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 사용자 주문 취소 알림 이벤트 생성
     */
    public static OrderNotificationEvent createCancelledUserNotification(String orderId, Long userId, String storeName) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("CANCELLED_USER")
                .orderId(orderId)
                .userId(userId)
                .title("주문 취소")
                .message(storeName + " 매장 주문이 취소되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 관리자 주문 취소 알림 이벤트 생성
     */
    public static OrderNotificationEvent createCancelledAdminNotification(String orderId, Long userId, String storeName) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("CANCELLED_ADMIN")
                .orderId(orderId)
                .userId(userId)
                .title("주문 취소")
                .message(storeName + " 매장 주문이 관리자에 의해 취소되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 주문 준비 중 알림 이벤트 생성
     */
    public static OrderNotificationEvent createPreparingNotification(String orderId, Long userId, String storeName) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("PREPARING")
                .orderId(orderId)
                .userId(userId)
                .title("주문 수락")
                .message(storeName + " 매장에서 주문을 수락했습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 픽업 준비 완료 알림 이벤트 생성
     */
    public static OrderNotificationEvent createPreparedNotification(String orderId, Long userId, String storeName) {
        return OrderNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationType("PREPARED")
                .orderId(orderId)
                .userId(userId)
                .title("픽업 준비 완료")
                .message(storeName + " 매장에서 픽업준비가 완료되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
