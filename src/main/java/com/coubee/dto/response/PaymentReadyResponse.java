package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentReadyResponse(
        @Schema(description = "Buyer name", example = "John Doe") String buyerName,
        @Schema(description = "Payment item name (First product + N more)", example = "Regular Jeans and 1 more") String name,
        @Schema(description = "Total payment amount", example = "206000") int amount,
        @Schema(
                        description = "Unique payment request ID (merchant_uid)",
                        example = "popup_1_order_6ef377d3-e92c-46e6-b38a-eb87227da444")
                String merchantUid) {
    public static PaymentReadyResponse of(
            String buyerName, String name, int amount, String merchantUid) {
        return new PaymentReadyResponse(buyerName, name, amount, merchantUid);
    }
} 
