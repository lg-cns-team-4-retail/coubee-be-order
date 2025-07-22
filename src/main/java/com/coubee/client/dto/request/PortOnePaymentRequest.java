package com.coubee.client.dto.request;

import lombok.Builder;
import lombok.Getter;

/**
 * PortOne Payment Preparation Request DTO
 */
@Getter
@Builder
public class PortOnePaymentRequest {

    /**
     * Payment ID (same as Order ID)
     */
    private String paymentId;

    /**
     * Payment amount
     */
    private Integer amount;

    /**
     * Order name
     */
    private String orderName;

    /**
     * Buyer name
     */
    private String buyerName;

    /**
     * Buyer email
     */
    private String buyerEmail;

    /**
     * Buyer phone number
     */
    private String buyerTel;
} 
