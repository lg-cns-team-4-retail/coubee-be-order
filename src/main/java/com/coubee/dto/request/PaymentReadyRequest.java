package com.coubee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PaymentReadyRequest(
        @NotNull(message = "Store ID cannot be empty") @Schema(description = "Store ID", example = "1")
                Long storeId,
        @NotNull(message = "Item list cannot be empty") @Schema(description = "Payment item list")
                List<Item> items) {
    @Schema(description = "Individual item information for payment")
    public record Item(
            @NotNull(message = "Item ID cannot be empty") @Schema(description = "Item ID", example = "11")
                    Long itemId,
            @NotNull(message = "Purchase quantity cannot be empty") @Schema(description = "Purchase quantity", example = "2")
                    Integer quantity) {}
} 
