package com.coubee.dto;

import java.time.LocalDateTime;

public record FlatPaymentItem(
        Long paymentId,
        Long storeId,
        LocalDateTime paidAt,
        String itemName,
        int quantity,
        int price) {} 
