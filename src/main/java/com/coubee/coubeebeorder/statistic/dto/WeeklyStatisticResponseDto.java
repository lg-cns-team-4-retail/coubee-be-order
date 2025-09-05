package com.coubee.coubeebeorder.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "Comprehensive weekly sales statistics response with Hotdeal breakdown")
public class WeeklyStatisticResponseDto {

    @Schema(description = "Week start date", example = "2023-05-29")
    private LocalDate weekStartDate;

    @Schema(description = "Week end date", example = "2023-06-04")
    private LocalDate weekEndDate;

    // Overall Summary
    @Schema(description = "Overall summary statistics")
    private OverallSummary overallSummary;

    // Hotdeal vs Regular Analysis
    @Schema(description = "Hotdeal vs Regular sales breakdown")
    private HotdealAnalysis hotdealAnalysis;

    // 차트용 일별 분석
    @Schema(description = "Daily breakdown of sales for chart display")
    private List<DailyBreakdown> dailyBreakdown;

    // 전체 판매 상품 목록
    @Schema(description = "Complete list of sold items with details")
    private List<SoldItemSummary> soldItemsSummary;

    @Schema(description = "Change rate compared to previous week (%)", example = "8.3")
    private Double changeRate;

    @Getter
    @Builder
    @Schema(description = "Overall summary statistics")
    public static class OverallSummary {
        @Schema(description = "Total sales amount for the week", example = "1050000")
        private Long totalSalesAmount;

        @Schema(description = "Total number of orders for the week", example = "175")
        private Integer totalOrderCount;

        @Schema(description = "Total number of items sold for the week", example = "315")
        private Integer totalItemCount;

        @Schema(description = "Average daily sales amount for the week", example = "150000")
        private Long averageDailySalesAmount;

        @Schema(description = "Number of unique customers for the week", example = "140")
        private Integer uniqueCustomerCount;

        @Schema(description = "Best performing day of the week", example = "FRIDAY")
        private String bestPerformingDay;

        @Schema(description = "Sales amount on best performing day", example = "200000")
        private Long bestDaySalesAmount;
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
        @Schema(description = "Total sales amount from hotdeal items", example = "665000")
        private Long salesAmount;

        @Schema(description = "Number of orders containing hotdeal items", example = "105")
        private Integer orderCount;

        @Schema(description = "Total quantity of hotdeal items sold", example = "200")
        private Integer itemCount;

        @Schema(description = "Average hotdeal order amount", example = "6333")
        private Long averageOrderAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Regular sales statistics")
    public static class RegularStats {
        @Schema(description = "Total sales amount from regular items", example = "385000")
        private Long salesAmount;

        @Schema(description = "Number of orders with only regular items", example = "70")
        private Integer orderCount;

        @Schema(description = "Total quantity of regular items sold", example = "115")
        private Integer itemCount;

        @Schema(description = "Average regular order amount", example = "5500")
        private Long averageOrderAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Daily breakdown for chart display")
    public static class DailyBreakdown {
        @Schema(description = "Day of the week", example = "MONDAY")
        private String dayOfWeek;

        @Schema(description = "Date", example = "2023-05-29")
        private LocalDate date;

        @Schema(description = "Total sales amount for the day", example = "120000")
        private Long salesAmount;

        @Schema(description = "Number of orders for the day", example = "20")
        private Integer orderCount;

        @Schema(description = "Hotdeal sales amount for the day", example = "75000")
        private Long hotdealSalesAmount;

        @Schema(description = "Regular sales amount for the day", example = "45000")
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

        @Schema(description = "Total quantity sold", example = "35")
        private Integer totalQuantity;

        @Schema(description = "Total revenue from this product", example = "175000")
        private Long totalRevenue;

        @Schema(description = "Quantity sold as hotdeal items", example = "21")
        private Integer hotdealQuantity;

        @Schema(description = "Revenue from hotdeal sales", example = "105000")
        private Long hotdealRevenue;

        @Schema(description = "Quantity sold as regular items", example = "14")
        private Integer regularQuantity;

        @Schema(description = "Revenue from regular sales", example = "70000")
        private Long regularRevenue;

        @Schema(description = "Whether this product was sold during hotdeal", example = "true")
        private Boolean wasPartOfHotdeal;
    }
}
