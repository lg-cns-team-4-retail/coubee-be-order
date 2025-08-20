package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;

import java.time.LocalDate;

/**
 * Service interface for sales statistics business logic
 */
public interface StatisticService {

    /**
     * Get daily sales statistics for a specific date
     *
     * @param date the date to get statistics for
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return daily statistics data
     */
    DailyStatisticDto dailyStatistic(LocalDate date, Long storeId);

    /**
     * Get weekly sales statistics for a specific week
     *
     * @param weekStartDate the start date of the week
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return weekly statistics data
     */
    WeeklyStatisticDto weeklyStatistic(LocalDate weekStartDate, Long storeId);

    /**
     * Get monthly sales statistics for a specific month
     *
     * @param year the year
     * @param month the month (1-12)
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return monthly statistics data
     */
    MonthlyStatisticDto monthlyStatistic(int year, int month, Long storeId);
}
