package com.coubee.coubeebeorder.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING("Pending"),
    PAID("Paid"),
    PREPARING("Preparing"),
    PREPARED("Prepared"),
    RECEIVED("Received"),
//    CANCELLED("Cancelled"),
    CANCELLED_USER("Cancelled by User"),
    CANCELLED_ADMIN("Cancelled by Admin"),
    FAILED("Failed");

    private final String description;
}