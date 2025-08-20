package com.coubee.coubeebeorder.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "Daily sales statistics response")
public class DailyStatisticDto {

    @Schema(description = "Statistics date", example = "2023-06-01")
    private LocalDate date;

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

    @Schema(description = "Change rate compared to previous day (%)", example = "12.5")
    private Double changeRate;

    @Schema(description = "Peak hour of the day (24-hour format)", example = "14")
    private Integer peakHour;

    @Schema(description = "Sales amount during peak hour", example = "25000")
    private Long peakHourSalesAmount;
}
