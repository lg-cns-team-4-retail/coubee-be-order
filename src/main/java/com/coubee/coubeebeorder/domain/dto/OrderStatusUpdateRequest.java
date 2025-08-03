package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Order status update request")
public class OrderStatusUpdateRequest {

    @Schema(description = "New order status",
            example = "PREPARING",
            allowableValues = {"PENDING", "PAID", "PREPARING", "PREPARED", "RECEIVED", "CANCELLED", "FAILED"})
    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Schema(description = "Reason for status change (optional)",
            example = "Started food preparation")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}