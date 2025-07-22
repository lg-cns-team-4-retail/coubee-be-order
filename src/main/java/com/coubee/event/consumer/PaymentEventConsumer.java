package com.coubee.event.consumer;

import com.coubee.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for processing payment events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    /**
     * Process payment events from Kafka topic
     *
     * @param paymentEvent Payment event payload
     * @param partition Kafka partition
     * @param offset Kafka offset
     * @param acknowledgment Manual acknowledgment
     */
    @KafkaListener(topics = "payment-events", groupId = "payment-service-group")
    public void handlePaymentEvent(
            @Payload PaymentEvent paymentEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing payment event: eventType={}, paymentId={}, orderId={}, partition={}, offset={}", 
                    paymentEvent.getEventType(), paymentEvent.getPaymentId(), 
                    paymentEvent.getOrderId(), partition, offset);

            // Process the payment event based on event type
            switch (paymentEvent.getEventType()) {
                case "PAYMENT_COMPLETED":
                    handlePaymentCompleted(paymentEvent);
                    break;
                case "PAYMENT_FAILED":
                    handlePaymentFailed(paymentEvent);
                    break;
                case "PAYMENT_CANCELLED":
                    handlePaymentCancelled(paymentEvent);
                    break;
                default:
                    log.warn("Unknown payment event type: {}", paymentEvent.getEventType());
            }

            // Manually acknowledge the message after successful processing
            acknowledgment.acknowledge();
            
            log.info("Payment event processed successfully: eventId={}", paymentEvent.getEventId());

        } catch (Exception e) {
            log.error("Error processing payment event: eventId={}, error={}", 
                    paymentEvent.getEventId(), e.getMessage(), e);
            
            // Don't acknowledge on error - this will trigger retry or send to DLQ
            // depending on Kafka configuration
        }
    }

    private void handlePaymentCompleted(PaymentEvent paymentEvent) {
        log.info("Handling PAYMENT_COMPLETED event: paymentId={}, orderId={}, amount={}, pgProvider={}", 
                paymentEvent.getPaymentId(), paymentEvent.getOrderId(), 
                paymentEvent.getAmount(), paymentEvent.getPgProvider());
        
        // Add business logic for payment completion event
        // e.g., update order status, send confirmation, trigger fulfillment, etc.
    }

    private void handlePaymentFailed(PaymentEvent paymentEvent) {
        log.info("Handling PAYMENT_FAILED event: paymentId={}, orderId={}, amount={}, pgProvider={}", 
                paymentEvent.getPaymentId(), paymentEvent.getOrderId(), 
                paymentEvent.getAmount(), paymentEvent.getPgProvider());
        
        // Add business logic for payment failure event
        // e.g., cancel order, send notification, restore inventory, etc.
    }

    private void handlePaymentCancelled(PaymentEvent paymentEvent) {
        log.info("Handling PAYMENT_CANCELLED event: paymentId={}, orderId={}, amount={}, pgProvider={}", 
                paymentEvent.getPaymentId(), paymentEvent.getOrderId(), 
                paymentEvent.getAmount(), paymentEvent.getPgProvider());
        
        // Add business logic for payment cancellation event
        // e.g., process refund, update order status, send notification, etc.
    }
}