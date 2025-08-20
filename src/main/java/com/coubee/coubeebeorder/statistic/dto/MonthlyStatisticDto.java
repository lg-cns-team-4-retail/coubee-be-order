package com.coubee.coubeebeorder.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "Monthly sales statistics response")
public class MonthlyStatisticDto {

    @Schema(description = "Month and year", example = "2023-06")
    private String month;

    @Schema(description = "Month start date", example = "2023-06-01")
    private LocalDate monthStartDate;

    @Schema(description = "Month end date", example = "2023-06-30")
    private LocalDate monthEndDate;

    @Schema(description = "Total sales amount for the month", example = "4500000")
    private Long totalSalesAmount;

    @Schema(description = "Total number of orders for the month", example = "750")
    private Integer totalOrderCount;

    @Schema(description = "Total number of items sold for the month", example = "1350")
    private Integer totalItemCount;

    @Schema(description = "Average daily sales amount for the month", example = "150000")
    private Long averageDailySalesAmount;

    @Schema(description = "Number of unique customers for the month", example = "600")
    private Integer uniqueCustomerCount;

    @Schema(description = "Growth rate compared to previous month (%)", example = "12.5")
    private Double growthRate;

    @Schema(description = "Best performing week of the month", example = "2023-06-12 to 2023-06-18")
    private String bestPerformingWeek;

    @Schema(description = "Sales amount during best performing week", example = "1200000")
    private Long bestWeekSalesAmount;

    @Schema(description = "Top selling products for the month")
    private List<TopProduct> topProducts;

    @Schema(description = "Weekly breakdown of sales statistics")
    private List<WeeklyBreakdown> weeklyBreakdown;

    @Getter
    @Builder
    @Schema(description = "Top selling product information")
    public static class TopProduct {
        @Schema(description = "Product ID", example = "1")
        private Long productId;

        @Schema(description = "Product name", example = "테스트 상품 1")
        private String productName;

        @Schema(description = "Total quantity sold", example = "150")
        private Integer quantitySold;

        @Schema(description = "Total sales amount for this product", example = "300000")
        private Long salesAmount;
    }

    @Getter
    @Builder
    @Schema(description = "Weekly breakdown within the month")
    public static class WeeklyBreakdown {
        @Schema(description = "Week number in the month", example = "1")
        private Integer weekNumber;

        @Schema(description = "Week start date", example = "2023-06-01")
        private LocalDate weekStartDate;

        @Schema(description = "Week end date", example = "2023-06-07")
        private LocalDate weekEndDate;

        @Schema(description = "Sales amount for the week", example = "1050000")
        private Long salesAmount;

        @Schema(description = "Number of orders for the week", example = "175")
        private Integer orderCount;
    }
}
