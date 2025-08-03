package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Order status update response")
public class OrderStatusUpdateResponse {

    @Schema(description = "Order ID", example = "order_533d1397b23848638b6b177e93c941f9")
    private String orderId;

    @Schema(description = "Previous status", example = "PAID")
    private OrderStatus previousStatus;

    @Schema(description = "Current status", example = "PREPARING")
    private OrderStatus currentStatus;

    @Schema(description = "Reason for status change", example = "Started food preparation")
    private String reason;

    @Schema(description = "Updated timestamp", example = "2025-01-23T10:05:25")
    private LocalDateTime updatedAt;

    @Schema(description = "User ID who updated the status", example = "123")
    private Long updatedByUserId;
}