package com.coubee.client.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PortOne Payment Cancellation Response DTO
 */
@Getter
@Setter
public class PortOnePaymentCancelResponse {

    /**
     * Payment ID
     */
    private String paymentId;

    /**
     * Cancellation amount
     */
    private Integer cancelAmount;

    /**
     * Cancellation time
     */
    private LocalDateTime canceledAt;

    /**
     * Cancellation reason
     */
    private String cancelReason;

    /**
     * Payment status
     */
    private String status;
} 
