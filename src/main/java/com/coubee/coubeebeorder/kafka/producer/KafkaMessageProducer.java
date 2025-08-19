package com.coubee.coubeebeorder.kafka.producer;

import com.coubee.coubeebeorder.kafka.producer.product.event.StockDecreaseEvent;
import com.coubee.coubeebeorder.kafka.producer.product.event.StockIncreaseEvent;
import com.coubee.coubeebeorder.kafka.producer.notification.event.OrderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 메시지 프로듀서
 * 새로운 패키지 컨벤션에 따라 타겟 서비스별로 이벤트를 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev")
public class KafkaMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 타겟 서비스별 토픽 정의
    private static final String PRODUCT_SERVICE_TOPIC = "product-events";
    private static final String NOTIFICATION_SERVICE_TOPIC = "notification-events";

    /**
     * Product Service로 재고 감소 이벤트 발행
     */
    public void publishStockDecreaseEvent(StockDecreaseEvent event) {
        try {
            log.info("재고 감소 이벤트 발행: eventId={}, orderId={}", 
                    event.getEventId(), event.getOrderId());

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(PRODUCT_SERVICE_TOPIC, event.getOrderId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("재고 감소 이벤트 발행 성공: eventId={}, topic={}, partition={}, offset={}", 
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("재고 감소 이벤트 발행 실패: eventId={}, error={}", 
                            event.getEventId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("재고 감소 이벤트 발행 중 오류: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Product Service로 재고 증가 이벤트 발행
     */
    public void publishStockIncreaseEvent(StockIncreaseEvent event) {
        try {
            log.info("재고 증가 이벤트 발행: eventId={}, orderId={}", 
                    event.getEventId(), event.getOrderId());

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(PRODUCT_SERVICE_TOPIC, event.getOrderId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("재고 증가 이벤트 발행 성공: eventId={}, topic={}, partition={}, offset={}", 
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("재고 증가 이벤트 발행 실패: eventId={}, error={}", 
                            event.getEventId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("재고 증가 이벤트 발행 중 오류: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Notification Service로 주문 알림 이벤트 발행
     */
    public void publishOrderNotificationEvent(OrderNotificationEvent event) {
        try {
            log.info("주문 알림 이벤트 발행: eventId={}, orderId={}, type={}", 
                    event.getEventId(), event.getOrderId(), event.getNotificationType());

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(NOTIFICATION_SERVICE_TOPIC, event.getOrderId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("주문 알림 이벤트 발행 성공: eventId={}, topic={}, partition={}, offset={}", 
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("주문 알림 이벤트 발행 실패: eventId={}, error={}", 
                            event.getEventId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("주문 알림 이벤트 발행 중 오류: eventId={}, error={}", 
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 범용 메시지 발행 메서드 (하위 호환성 유지)
     */
    public void publishMessage(String topic, String key, Object message) {
        try {
            log.info("메시지 발행: topic={}, key={}", topic, key);

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("메시지 발행 성공: topic={}, partition={}, offset={}", 
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("메시지 발행 실패: topic={}, error={}", topic, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("메시지 발행 중 오류: topic={}, error={}", topic, e.getMessage(), e);
        }
    }
}
