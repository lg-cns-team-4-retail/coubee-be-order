package com.coubee.client.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PortOne Payment Response DTO
 */
@Getter
@Setter
public class PortOnePaymentResponse {

    /**
     * Payment ID
     */
    private String paymentId;

    /**
     * PG Provider
     */
    private String pgProvider;

    /**
     * PG Transaction ID
     */
    private String pgTid;

    /**
     * Payment method
     */
    private String method;

    /**
     * Payment amount
     */
    private Integer amount;

    /**
     * Payment status
     */
    private String status;

    /**
     * Payment completion time
     */
    private LocalDateTime paidAt;

    /**
     * Payment failure time
     */
    private LocalDateTime failedAt;

    /**
     * Payment failure reason
     */
    private String failReason;

    /**
     * Receipt URL
     */
    private String receiptUrl;

    /**
     * Order name
     */
    private String orderName;

    /**
     * Buyer name
     */
    private String buyerName;
} 
