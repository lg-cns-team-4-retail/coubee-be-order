package com.coubee.coubeebeorder.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "Comprehensive daily sales statistics response with Hotdeal breakdown")
public class DailyStatisticResponseDto {

    @Schema(description = "Statistics date", example = "2023-06-01")
    private LocalDate date;

    // Overall Summary
    @Schema(description = "Overall summary statistics")
    private OverallSummary overallSummary;

    // Hotdeal vs Regular Analysis
    @Schema(description = "Hotdeal vs Regular sales breakdown")
    private HotdealAnalysis hotdealAnalysis;

    // 차트용 시간별 분석
    @Schema(description = "Hourly breakdown of sales for chart display")
    private List<HourlyBreakdown> hourlyBreakdown;

    // 전체 판매 상품 목록
    @Schema(description = "Complete list of sold items with details")
    private List<SoldItemSummary> soldItemsSummary;

    @Schema(description = "Change rate compared to previous day (%)", example = "8.3")
    private Double changeRate;

    @Getter
    @Builder
    @Schema(description = "Overall summary statistics")
    public static class OverallSummary {
        @Schema(description = "Total sales amount for the day", example = "150000")
        private Long totalSalesAmount;

        @Schema(description = "Total number of orders for the day", example = "25")
        private Integer totalOrderCount;

        @Schema(description = "Total number of items sold for the day", example = "45")
        private Integer totalItemCount;

        @Schema(description = "Average order amount for the day", example = "6000")
        private Long averageOrderAmount;

        @Schema(description = "Number of unique customers for the day", example = "20")
        private Integer uniqueCustomerCount;

        @Schema(description = "Peak hour of the day (0-23)", example = "14")
        private Integer peakHour;

        @Schema(description = "Sales amount during peak hour", example = "25000")
        private Long peakHourSalesAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Hotdeal vs Regular sales analysis")
    public static class HotdealAnalysis {
        @Schema(description = "Hotdeal sales statistics")
        private HotdealStats hotdealSales;

        @Schema(description = "Regular sales statistics")
        private RegularStats regularSales;

        @Schema(description = "Hotdeal conversion rate (%)", example = "65.5")
        private Double hotdealConversionRate;
    }

    @Getter
    @Builder
    @Schema(description = "Hotdeal sales statistics")
    public static class HotdealStats {
        @Schema(description = "Total sales amount from hotdeal items", example = "95000")
        private Long salesAmount;

        @Schema(description = "Number of orders containing hotdeal items", example = "15")
        private Integer orderCount;

        @Schema(description = "Total quantity of hotdeal items sold", example = "28")
        private Integer itemCount;

        @Schema(description = "Average hotdeal order amount", example = "6333")
        private Long averageOrderAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Regular sales statistics")
    public static class RegularStats {
        @Schema(description = "Total sales amount from regular items", example = "55000")
        private Long salesAmount;

        @Schema(description = "Number of orders with only regular items", example = "10")
        private Integer orderCount;

        @Schema(description = "Total quantity of regular items sold", example = "17")
        private Integer itemCount;

        @Schema(description = "Average regular order amount", example = "5500")
        private Long averageOrderAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Hourly breakdown for chart display")
    public static class HourlyBreakdown {
        @Schema(description = "Hour of the day (0-23)", example = "14")
        private Integer hour;

        @Schema(description = "Total sales amount for this hour", example = "25000")
        private Long salesAmount;

        @Schema(description = "Number of orders for this hour", example = "5")
        private Integer orderCount;

        @Schema(description = "Hotdeal sales amount for this hour", example = "15000")
        private Long hotdealSalesAmount;

        @Schema(description = "Regular sales amount for this hour", example = "10000")
        private Long regularSalesAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Individual sold item summary")
    public static class SoldItemSummary {
        @Schema(description = "Product ID", example = "1")
        private Long productId;

        @Schema(description = "Product name", example = "Test Product 1")
        private String productName;

        @Schema(description = "Total quantity sold", example = "5")
        private Integer totalQuantity;

        @Schema(description = "Total revenue from this product", example = "25000")
        private Long totalRevenue;

        @Schema(description = "Quantity sold as hotdeal items", example = "3")
        private Integer hotdealQuantity;

        @Schema(description = "Revenue from hotdeal sales", example = "15000")
        private Long hotdealRevenue;

        @Schema(description = "Quantity sold as regular items", example = "2")
        private Integer regularQuantity;

        @Schema(description = "Revenue from regular sales", example = "10000")
        private Long regularRevenue;

        @Schema(description = "Whether this product was sold during hotdeal", example = "true")
        private Boolean wasPartOfHotdeal;
    }
}
