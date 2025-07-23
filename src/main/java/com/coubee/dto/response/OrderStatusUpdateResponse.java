package com.coubee.dto.response;

import com.coubee.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Order Status Update Response DTO
 */
@Getter
@Builder
public class OrderStatusUpdateResponse {

    @Schema(description = "Order ID", example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
    private String orderId;

    @Schema(description = "Previous status", example = "PAID")
    private OrderStatus previousStatus;

    @Schema(description = "Current status", example = "RECEIVED")
    private OrderStatus currentStatus;

    @Schema(description = "Updated timestamp", example = "2023-11-16T14:00:00Z")
    private LocalDateTime updatedAt;
} 