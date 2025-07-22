package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentReadyResponse(
        @Schema(description = "Buyer name", example = "홍길동") String buyerName,
        @Schema(description = "Payment item name (First product + N more)", example = "테스트 상품 1 외 1건") String name,
        @Schema(description = "Total payment amount", example = "200") int amount,
        @Schema(
                        description = "Unique payment request ID (merchant_uid)",
                        example = "order_b7833686f25b48e0862612345678abcd")
                String merchantUid) {
    public static PaymentReadyResponse of(
            String buyerName, String name, int amount, String merchantUid) {
        return new PaymentReadyResponse(buyerName, name, amount, merchantUid);
    }
} 
