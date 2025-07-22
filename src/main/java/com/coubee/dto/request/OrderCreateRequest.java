package com.coubee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Order Creation Request DTO
 */
@Getter
@Builder
public class OrderCreateRequest {

    @Schema(description = "Store ID", example = "1")
    @NotNull(message = "Store ID is required")
    private Long storeId;

    @Schema(description = "Recipient name", example = "홍길동")
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @Schema(description = "Payment method", example = "card")
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Schema(description = "Order item list")
    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * Order Item Request DTO
     */
    @Getter
    @Builder
    public static class OrderItemRequest {

        @Schema(description = "Product ID", example = "1")
        @NotNull(message = "Product ID is required")
        private Long productId;

        @Schema(description = "Order quantity", example = "2")
        @NotNull(message = "Order quantity is required")
        @Min(value = 1, message = "Order quantity must be at least 1")
        private Integer quantity;
    }
} 
