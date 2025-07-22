package com.coubee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payment event class for Kafka messaging
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    
    private String eventId;
    private String eventType;
    private String paymentId;
    private String orderId;
    private Long amount;
    private String paymentStatus;
    private String pgProvider;
    private LocalDateTime timestamp;
    
    public static PaymentEvent createPaymentCompleted(String paymentId, String orderId, Long amount, String pgProvider) {
        return new PaymentEvent(
            java.util.UUID.randomUUID().toString(),
            "PAYMENT_COMPLETED",
            paymentId,
            orderId,
            amount,
            "PAID",
            pgProvider,
            LocalDateTime.now()
        );
    }
    
    public static PaymentEvent createPaymentFailed(String paymentId, String orderId, Long amount, String pgProvider) {
        return new PaymentEvent(
            java.util.UUID.randomUUID().toString(),
            "PAYMENT_FAILED",
            paymentId,
            orderId,
            amount,
            "FAILED",
            pgProvider,
            LocalDateTime.now()
        );
    }
    
    public static PaymentEvent createPaymentCancelled(String paymentId, String orderId, Long amount, String pgProvider) {
        return new PaymentEvent(
            java.util.UUID.randomUUID().toString(),
            "PAYMENT_CANCELLED",
            paymentId,
            orderId,
            amount,
            "CANCELLED",
            pgProvider,
            LocalDateTime.now()
        );
    }
}