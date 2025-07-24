package com.coubee.coubeebeorder.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

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