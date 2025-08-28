package com.coubee.coubeebeorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User order summary response containing aggregated order statistics")
public class UserOrderSummaryDto {

    @Schema(description = "Total number of valid orders", example = "15")
    private Long totalOrderCount;

    @Schema(description = "Total original amount before discounts", example = "150000")
    private Long totalOriginalAmount;

    @Schema(description = "Total discount amount applied", example = "25000")
    private Long totalDiscountAmount;

    @Schema(description = "Final purchase amount after discounts", example = "125000")
    private Long finalPurchaseAmount;
}
