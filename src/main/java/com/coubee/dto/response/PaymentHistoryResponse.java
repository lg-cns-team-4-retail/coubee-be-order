package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentHistoryResponse(
        @Schema(description = "Payment ID", example = "1") Long paymentId,
        @Schema(description = "Store ID", example = "1") Long storeId,
        @Schema(description = "Payment completion time", example = "2024-05-31T14:00:00") LocalDateTime paidAt,
        @Schema(description = "List of items included in the payment") List<Item> items) {
    public record Item(
            @Schema(description = "Item name", example = "DAZED Magazine") String itemName,
            @Schema(description = "Purchase quantity", example = "1") int quantity,
            @Schema(description = "Item payment amount", example = "15000") int price) {}
} 
