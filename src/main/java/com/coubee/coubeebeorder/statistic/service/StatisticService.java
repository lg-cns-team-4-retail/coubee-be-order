package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.statistic.dto.DailyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.dto.ProductSalesSummaryDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticResponseDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for sales statistics business logic
 */
public interface StatisticService {



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

    // ========================================
    // 핫딜 지원이 포함된 향상된 통계 메소드
    // ========================================

    /**
     * Get comprehensive daily sales statistics with hotdeal breakdown
     *
     * @param date the date to get statistics for
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @param userId the user ID making the request (for ownership validation)
     * @return comprehensive daily statistics data with hotdeal analysis
     */
    DailyStatisticResponseDto getDailyStatisticsWithHotdeal(LocalDate date, Long storeId, Long userId);

    /**
     * Get comprehensive weekly sales statistics with hotdeal breakdown
     *
     * @param weekStartDate the start date of the week
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @param userId the user ID making the request (for ownership validation)
     * @return comprehensive weekly statistics data with hotdeal analysis
     */
    WeeklyStatisticResponseDto getWeeklyStatisticsWithHotdeal(LocalDate weekStartDate, Long storeId, Long userId);

    /**
     * Get comprehensive monthly sales statistics with hotdeal breakdown
     *
     * @param year the year
     * @param month the month (1-12)
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @param userId the user ID making the request (for ownership validation)
     * @return comprehensive monthly statistics data with hotdeal analysis
     */
    MonthlyStatisticResponseDto getMonthlyStatisticsWithHotdeal(int year, int month, Long storeId, Long userId);
}
