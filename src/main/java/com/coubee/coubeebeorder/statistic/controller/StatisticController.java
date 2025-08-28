package com.coubee.coubeebeorder.statistic.controller;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.ProductSalesSummaryDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;
import com.coubee.coubeebeorder.statistic.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Statistic API", description = "APIs for retrieving sales statistics and reports")
@RestController
@RequestMapping("/api/order/reports/admin")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @Operation(summary = "Get Daily Sales Statistics", description = "Retrieves daily sales statistics for a specific date (Store Owner only)")
    @GetMapping("/sales/daily")
    public ApiResponseDto<DailyStatisticDto> getDailyStatistics(
            @Parameter(description = "User ID from authentication", hidden = true)
            @RequestHeader("X-Auth-UserId") Long userId,
            @Parameter(description = "Date for statistics", required = true, example = "2023-06-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Store ID for filtering (required)", required = true, example = "1")
            @RequestParam Long storeId) {

        DailyStatisticDto response = statisticService.dailyStatistic(date, storeId, userId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get Weekly Sales Statistics", description = "Retrieves weekly sales statistics for a specific week (Store Owner only)")
    @GetMapping("/sales/weekly")
    public ApiResponseDto<WeeklyStatisticDto> getWeeklyStatistics(
            @Parameter(description = "User ID from authentication", hidden = true)
            @RequestHeader("X-Auth-UserId") Long userId,
            @Parameter(description = "Week start date", required = true, example = "2023-05-29")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @Parameter(description = "Store ID for filtering (required)", required = true, example = "1")
            @RequestParam Long storeId) {

        WeeklyStatisticDto response = statisticService.weeklyStatistic(weekStartDate, storeId, userId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get Monthly Sales Statistics", description = "Retrieves monthly sales statistics for a specific month (Store Owner only)")
    @GetMapping("/sales/monthly")
    public ApiResponseDto<MonthlyStatisticDto> getMonthlyStatistics(
            @Parameter(description = "User ID from authentication", hidden = true)
            @RequestHeader("X-Auth-UserId") Long userId,
            @Parameter(description = "Year", required = true, example = "2023")
            @RequestParam int year,
            @Parameter(description = "Month (1-12)", required = true, example = "6")
            @RequestParam int month,
            @Parameter(description = "Store ID for filtering (required)", required = true, example = "1")
            @RequestParam Long storeId) {

        MonthlyStatisticDto response = statisticService.monthlyStatistic(year, month, storeId, userId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get Product Sales Summary", description = "Retrieves product sales summary for a store within a date range (Store Owner only)")
    @GetMapping("/product-sales-summary")
    public ApiResponseDto<List<ProductSalesSummaryDto>> getProductSalesSummary(
            @Parameter(description = "User ID from authentication", hidden = true)
            @RequestHeader("X-Auth-UserId") Long userId,
            @Parameter(description = "Store ID", required = true, example = "1")
            @RequestParam Long storeId,
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true, example = "2023-06-01")
            @RequestParam LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true, example = "2023-06-30")
            @RequestParam LocalDate endDate) {

        List<ProductSalesSummaryDto> response = statisticService.getProductSalesSummary(storeId, startDate, endDate, userId);
        return ApiResponseDto.readOk(response);
    }
}
