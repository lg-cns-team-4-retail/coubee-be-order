package com.coubee.dto.request;

import com.coubee.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Order Status Update Request DTO
 */
@Getter
@Builder
public class OrderStatusUpdateRequest {

    @Schema(description = "New order status", example = "RECEIVED")
    @NotNull(message = "Status is required")
    private OrderStatus status;
} 