package com.coubee.coubeebeorder.event.producer;

import com.coubee.coubeebeorder.domain.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev")
public class KafkaMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-events";
    private static final String PAYMENT_TOPIC = "payment-events";
    private static final String ITEM_PURCHASED_TOPIC = "item-purchased-topic";

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

    public void publishMessage(String topic, String key, Object message) {
        try {
            log.info("Publishing message to topic: {}, key: {}", topic, key);

            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Message published successfully: topic={}, partition={}, offset={}", 
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish message: topic={}, error={}", topic, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing message: topic={}, error={}", topic, e.getMessage(), e);
        }
    }
}