package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.ProductSalesSummaryDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for sales statistics business logic
 */
public interface StatisticService {

    /**
     * Get daily sales statistics for a specific date
     *
     * @param date the date to get statistics for
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @param userId the user ID making the request (for ownership validation)
     * @return daily statistics data
     */
    DailyStatisticDto dailyStatistic(LocalDate date, Long storeId, Long userId);

    /**
     * Get weekly sales statistics for a specific week
     *
     * @param weekStartDate the start date of the week
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @param userId the user ID making the request (for ownership validation)
     * @return weekly statistics data
     */
    WeeklyStatisticDto weeklyStatistic(LocalDate weekStartDate, Long storeId, Long userId);

    /**
     * Get monthly sales statistics for a specific month
     *
     * @param year the year
     * @param month the month (1-12)
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @param userId the user ID making the request (for ownership validation)
     * @return monthly statistics data
     */
    MonthlyStatisticDto monthlyStatistic(int year, int month, Long storeId, Long userId);

    /**
     * Get product sales summary for a store within a date range
     *
     * @param storeId the store ID to get product sales for
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @param userId the user ID making the request (for ownership validation)
     * @return list of product sales summary data
     */
    List<ProductSalesSummaryDto> getProductSalesSummary(Long storeId, LocalDate startDate, LocalDate endDate, Long userId);
}
