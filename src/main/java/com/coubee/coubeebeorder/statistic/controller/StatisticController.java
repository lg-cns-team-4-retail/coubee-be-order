package com.coubee.coubeebeorder.statistic.controller;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;
import com.coubee.coubeebeorder.statistic.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Statistic API", description = "APIs for retrieving sales statistics and reports")
@RestController
@RequestMapping("/api/reports/admin")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @Operation(summary = "Get Daily Sales Statistics", description = "Retrieves daily sales statistics for a specific date (Admin only)")
    @GetMapping("/sales/daily")
    public ApiResponseDto<DailyStatisticDto> getDailyStatistics(
            @Parameter(description = "User role from authentication", hidden = true)
            @RequestHeader("X-Auth-Role") String userRole,
            @Parameter(description = "Date for statistics", required = true, example = "2023-06-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Store ID for filtering (optional)", required = false, example = "1")
            @RequestParam(required = false) Long storeId) {

        // Validate user has permission to access statistics
        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new IllegalArgumentException("Only admins and super admins can access sales statistics");
        }

        DailyStatisticDto response = statisticService.dailyStatistic(date, storeId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get Weekly Sales Statistics", description = "Retrieves weekly sales statistics for a specific week (Admin only)")
    @GetMapping("/sales/weekly")
    public ApiResponseDto<WeeklyStatisticDto> getWeeklyStatistics(
            @Parameter(description = "User role from authentication", hidden = true)
            @RequestHeader("X-Auth-Role") String userRole,
            @Parameter(description = "Week start date", required = true, example = "2023-05-29")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @Parameter(description = "Store ID for filtering (optional)", required = false, example = "1")
            @RequestParam(required = false) Long storeId) {

        // Validate user has permission to access statistics
        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new IllegalArgumentException("Only admins and super admins can access sales statistics");
        }

        WeeklyStatisticDto response = statisticService.weeklyStatistic(weekStartDate, storeId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get Monthly Sales Statistics", description = "Retrieves monthly sales statistics for a specific month (Admin only)")
    @GetMapping("/sales/monthly")
    public ApiResponseDto<MonthlyStatisticDto> getMonthlyStatistics(
            @Parameter(description = "User role from authentication", hidden = true)
            @RequestHeader("X-Auth-Role") String userRole,
            @Parameter(description = "Year", required = true, example = "2023")
            @RequestParam int year,
            @Parameter(description = "Month (1-12)", required = true, example = "6")
            @RequestParam int month,
            @Parameter(description = "Store ID for filtering (optional)", required = false, example = "1")
            @RequestParam(required = false) Long storeId) {

        // Validate user has permission to access statistics
        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new IllegalArgumentException("Only admins and super admins can access sales statistics");
        }

        MonthlyStatisticDto response = statisticService.monthlyStatistic(year, month, storeId);
        return ApiResponseDto.readOk(response);
    }
}
