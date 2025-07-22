package com.coubee.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum representing payment status
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus {
    READY("Ready"),
    PAID("Paid"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    PARTIAL_CANCELLED("Partially Cancelled");

    private final String description;
} 
