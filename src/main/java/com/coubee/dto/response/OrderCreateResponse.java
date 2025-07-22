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

    @Schema(description = "Order ID", example = "order_b7833686f25b48e0862612345678abcd")
    private String orderId;

    @Schema(description = "Payment ID (used in PortOne payment window)", example = "order_b7833686f25b48e0862612345678abcd")
    private String paymentId;

    @Schema(description = "Payment amount", example = "200")
    private Integer amount;

    @Schema(description = "Order name (First product + N more)", example = "테스트 상품 1 외 1건")
    private String orderName;

    @Schema(description = "Buyer name", example = "홍길동")
    private String buyerName;
} 
