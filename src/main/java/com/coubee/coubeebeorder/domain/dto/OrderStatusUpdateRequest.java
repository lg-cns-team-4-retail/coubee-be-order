package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusUpdateRequest {

    @Schema(description = "New order status", example = "RECEIVED")
    @NotNull(message = "Status is required")
    private OrderStatus status;
}