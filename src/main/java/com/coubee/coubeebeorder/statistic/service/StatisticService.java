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
     * @return daily statistics data
     */
    DailyStatisticDto dailyStatistic(LocalDate date);

    /**
     * Get weekly sales statistics for a specific week
     *
     * @param weekStartDate the start date of the week
     * @return weekly statistics data
     */
    WeeklyStatisticDto weeklyStatistic(LocalDate weekStartDate);

    /**
     * Get monthly sales statistics for a specific month
     *
     * @param year the year
     * @param month the month (1-12)
     * @return monthly statistics data
     */
    MonthlyStatisticDto monthlyStatistic(int year, int month);
}
