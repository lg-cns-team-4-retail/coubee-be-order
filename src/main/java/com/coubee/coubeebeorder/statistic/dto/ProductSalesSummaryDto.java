package com.coubee.coubeebeorder.statistic.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSalesSummaryDto {
    private Long productId;
    private String productName;
    private Integer quantitySold;
    private Long totalSalesAmount;
}
