package com.coubee.client.dto.request;

import lombok.Builder;
import lombok.Getter;

/**
 * PortOne Payment Cancellation Request DTO
 */
@Getter
@Builder
public class PortOnePaymentCancelRequest {

    /**
     * Cancellation amount
     */
    private Integer cancelAmount;

    /**
     * Cancellation reason
     */
    private String cancelReason;
} 
