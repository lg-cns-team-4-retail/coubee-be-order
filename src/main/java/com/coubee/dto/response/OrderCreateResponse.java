package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Order Creation Response DTO
 */
@Getter
@Builder
public class OrderCreateResponse {

    @Schema(description = "Order ID", example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
    private String orderId;

    @Schema(description = "Payment ID (used in PortOne payment window)", example = "payment_01H1J5BFXCZDMG8RP0WCTFSN5Y")
    private String paymentId;

    @Schema(description = "Payment amount", example = "25000")
    private Integer amount;

    @Schema(description = "Order name (First product + N more)", example = "Men's T-shirt and 2 more")
    private String orderName;

    @Schema(description = "Buyer name", example = "John Doe")
    private String buyerName;
} 
