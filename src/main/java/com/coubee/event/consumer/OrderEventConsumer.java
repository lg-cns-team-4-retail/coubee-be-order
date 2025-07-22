package com.coubee.event.consumer;

import com.coubee.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing order events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    /**
     * Process order events from Kafka topic
     *
     * @param orderEvent Order event payload
     * @param partition Kafka partition
     * @param offset Kafka offset
     * @param acknowledgment Manual acknowledgment
     */
    @KafkaListener(topics = "order-events", groupId = "order-service-group")
    public void handleOrderEvent(
            @Payload OrderEvent orderEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing order event: eventType={}, orderId={}, partition={}, offset={}", 
                    orderEvent.getEventType(), orderEvent.getOrderId(), partition, offset);

            // Process the order event based on event type
            switch (orderEvent.getEventType()) {
                case "ORDER_CREATED":
                    handleOrderCreated(orderEvent);
                    break;
                case "ORDER_COMPLETED":
                    handleOrderCompleted(orderEvent);
                    break;
                case "ORDER_CANCELLED":
                    handleOrderCancelled(orderEvent);
                    break;
                default:
                    log.warn("Unknown order event type: {}", orderEvent.getEventType());
            }

            // Manually acknowledge the message after successful processing
            acknowledgment.acknowledge();
            
            log.info("Order event processed successfully: eventId={}", orderEvent.getEventId());

        } catch (Exception e) {
            log.error("Error processing order event: eventId={}, error={}", 
                    orderEvent.getEventId(), e.getMessage(), e);
            
            // Don't acknowledge on error - this will trigger retry or send to DLQ
            // depending on Kafka configuration
        }
    }

    private void handleOrderCreated(OrderEvent orderEvent) {
        log.info("Handling ORDER_CREATED event: orderId={}, userId={}, amount={}", 
                orderEvent.getOrderId(), orderEvent.getUserId(), orderEvent.getTotalAmount());
        
        // Add business logic for order creation event
        // e.g., send notification, update analytics, etc.
    }

    private void handleOrderCompleted(OrderEvent orderEvent) {
        log.info("Handling ORDER_COMPLETED event: orderId={}, userId={}, amount={}", 
                orderEvent.getOrderId(), orderEvent.getUserId(), orderEvent.getTotalAmount());
        
        // Add business logic for order completion event
        // e.g., send confirmation email, update inventory, etc.
    }

    private void handleOrderCancelled(OrderEvent orderEvent) {
        log.info("Handling ORDER_CANCELLED event: orderId={}, userId={}, amount={}", 
                orderEvent.getOrderId(), orderEvent.getUserId(), orderEvent.getTotalAmount());
        
        // Add business logic for order cancellation event
        // e.g., restore inventory, send notification, etc.
    }
}