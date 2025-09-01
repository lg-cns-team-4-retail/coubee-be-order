package com.coubee.coubeebeorder.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductSalesSummaryDto {

    @Schema(description = "Product ID", example = "5066")
    private Long productId;

    @Schema(description = "Product name", example = "[BestFarm] Premium Pork Belly 400g")
    private String productName;

    // 총 판매량 (translation: Total quantity sold (Hotdeal + Regular))
    @Schema(description = "Total quantity sold (Hotdeal + Regular)", example = "4")
    private Integer totalQuantitySold;

    // 총 판매 금액 (translation: Total sales amount)
    @Schema(description = "Total sales amount", example = "91200")
    private Long totalSalesAmount;

    // 핫딜 판매량 (translation: Hotdeal quantity sold)
    @Schema(description = "Hotdeal quantity sold", example = "1")
    private Integer hotdealQuantitySold;

    // 핫딜 판매 금액 (translation: Hotdeal sales amount)
    @Schema(description = "Hotdeal sales amount", example = "22800")
    private Long hotdealSalesAmount;

    // 일반 판매량 (translation: Regular quantity sold)
    @Schema(description = "Regular quantity sold", example = "3")
    private Integer regularQuantitySold;

    // 일반 판매 금액 (translation: Regular sales amount)
    @Schema(description = "Regular sales amount", example = "68400")
    private Long regularSalesAmount;
}
