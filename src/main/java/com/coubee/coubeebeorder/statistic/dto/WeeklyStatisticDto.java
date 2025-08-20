package com.coubee.coubeebeorder.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "Weekly sales statistics response")
public class WeeklyStatisticDto {

    @Schema(description = "Week start date", example = "2023-05-29")
    private LocalDate weekStartDate;

    @Schema(description = "Week end date", example = "2023-06-04")
    private LocalDate weekEndDate;

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

    @Schema(description = "Daily breakdown of sales statistics")
    private List<DailyBreakdown> dailyBreakdown;

    @Schema(description = "Change rate compared to previous week (%)", example = "8.3")
    private Double changeRate;

    @Getter
    @Builder
    @Schema(description = "Daily breakdown within the week")
    public static class DailyBreakdown {
        @Schema(description = "Day of the week", example = "MONDAY")
        private String dayOfWeek;

        @Schema(description = "Date", example = "2023-05-29")
        private LocalDate date;

        @Schema(description = "Sales amount for the day", example = "120000")
        private Long salesAmount;

        @Schema(description = "Number of orders for the day", example = "20")
        private Integer orderCount;
    }
}
