package com.coubee.event.producer;

import com.coubee.event.OrderEvent;
import com.coubee.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Event Producer for publishing order and payment events
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev") // dev 프로필이 아닐 때만 이 Bean을 생성
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-events";
    private static final String PAYMENT_TOPIC = "payment-events";

    /**
     * Publish order event to Kafka
     *
     * @param orderEvent Order event to publish
     */
    public void publishOrderEvent(OrderEvent orderEvent) {
        try {
            log.info("Publishing order event: eventType={}, orderId={}", 
                    orderEvent.getEventType(), orderEvent.getOrderId());

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(ORDER_TOPIC, orderEvent.getOrderId(), orderEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order event published successfully: eventId={}, topic={}, partition={}, offset={}", 
                            orderEvent.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish order event: eventId={}, error={}", 
                            orderEvent.getEventId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing order event: eventId={}, error={}", 
                    orderEvent.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Publish payment event to Kafka
     *
     * @param paymentEvent Payment event to publish
     */
    public void publishPaymentEvent(PaymentEvent paymentEvent) {
        try {
            log.info("Publishing payment event: eventType={}, paymentId={}", 
                    paymentEvent.getEventType(), paymentEvent.getPaymentId());

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(PAYMENT_TOPIC, paymentEvent.getPaymentId(), paymentEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Payment event published successfully: eventId={}, topic={}, partition={}, offset={}", 
                            paymentEvent.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish payment event: eventId={}, error={}", 
                            paymentEvent.getEventId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing payment event: eventId={}, error={}", 
                    paymentEvent.getEventId(), e.getMessage(), e);
        }
    }
}