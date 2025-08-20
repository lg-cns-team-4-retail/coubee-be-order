package com.coubee.coubeebeorder.statistic.repository;

import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;

import java.time.LocalDate;

/**
 * Repository interface for sales statistics data access
 */
public interface StatisticRepository {

    /**
     * Get daily sales statistics for a specific date
     *
     * @param date the date to get statistics for
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return daily statistics data
     */
    DailyStatisticDto getDailyStatistic(LocalDate date, Long storeId);

    /**
     * Get weekly sales statistics for a specific week
     *
     * @param weekStartDate the start date of the week
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return weekly statistics data
     */
    WeeklyStatisticDto getWeeklyStatistic(LocalDate weekStartDate, Long storeId);

    /**
     * Get monthly sales statistics for a specific month
     *
     * @param year the year
     * @param month the month (1-12)
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return monthly statistics data
     */
    MonthlyStatisticDto getMonthlyStatistic(int year, int month, Long storeId);
}
