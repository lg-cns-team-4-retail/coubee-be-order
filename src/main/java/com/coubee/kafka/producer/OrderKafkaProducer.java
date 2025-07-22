package com.coubee.kafka.producer;

import com.coubee.kafka.producer.notification.event.NotificationEvent;
import com.coubee.kafka.producer.stock.event.StockDecreaseEvent;
import com.coubee.kafka.producer.stock.event.StockIncreaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Order related Kafka event producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!dev") // dev 프로필이 아닐 때만 이 Bean을 생성
public class OrderKafkaProducer {

    private static final String STOCK_DECREASE_TOPIC = "stock-decrease";
    private static final String STOCK_INCREASE_TOPIC = "stock-increase";
    private static final String NOTIFICATION_TOPIC = "notification-create";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish stock decrease event
     *
     * @param event Stock decrease event
     */
    public void sendStockDecreaseEvent(StockDecreaseEvent event) {
        log.info("Sending stock decrease event: {}", event);
        kafkaTemplate.send(STOCK_DECREASE_TOPIC, event.getOrderId(), event);
    }

    /**
     * Publish stock increase event
     *
     * @param event Stock increase event
     */
    public void sendStockIncreaseEvent(StockIncreaseEvent event) {
        log.info("Sending stock increase event: {}", event);
        kafkaTemplate.send(STOCK_INCREASE_TOPIC, event.getOrderId(), event);
    }

    /**
     * Publish notification event
     *
     * @param event Notification event
     */
    public void sendNotificationEvent(NotificationEvent event) {
        log.info("Sending notification event: {}", event);
        kafkaTemplate.send(NOTIFICATION_TOPIC, event.getUserId().toString(), event);
    }
} 
