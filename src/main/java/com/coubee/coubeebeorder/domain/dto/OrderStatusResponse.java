package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Order status response")
public class OrderStatusResponse {

    @Schema(description = "Order ID", example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
    private String orderId;

    @Schema(description = "Current order status", example = "PAID")
    private OrderStatus status;
}
