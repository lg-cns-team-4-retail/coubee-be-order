package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;
import com.coubee.coubeebeorder.statistic.repository.StatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service implementation for sales statistics business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticServiceImpl implements StatisticService {

    private final StatisticRepository statisticRepository;

    @Override
    public DailyStatisticDto dailyStatistic(LocalDate date, Long storeId) {
        log.info("Getting daily statistics for date: {}, storeId: {}", date, storeId);

        try {
            // Get current day's statistics
            DailyStatisticDto todayStats = statisticRepository.getDailyStatistic(date, storeId);

            // Get previous day's statistics
            DailyStatisticDto yesterdayStats = statisticRepository.getDailyStatistic(date.minusDays(1), storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                todayStats.getTotalSalesAmount(),
                yesterdayStats.getTotalSalesAmount()
            );

            // Return DTO with the new field
            DailyStatisticDto result = DailyStatisticDto.builder()
                    .date(todayStats.getDate())
                    .totalSalesAmount(todayStats.getTotalSalesAmount())
                    .totalOrderCount(todayStats.getTotalOrderCount())
                    .totalItemCount(todayStats.getTotalItemCount())
                    .averageOrderAmount(todayStats.getAverageOrderAmount())
                    .uniqueCustomerCount(todayStats.getUniqueCustomerCount())
                    .peakHour(todayStats.getPeakHour())
                    .peakHourSalesAmount(todayStats.getPeakHourSalesAmount())
                    .changeRate(changeRate)
                    .build();

            log.info("Successfully retrieved daily statistics for date: {}, storeId: {}, total sales: {}, change rate: {}%",
                    date, storeId, result.getTotalSalesAmount(), changeRate);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving daily statistics for date: {}, storeId: {}", date, storeId, e);
            throw new RuntimeException("Failed to retrieve daily statistics", e);
        }
    }

    @Override
    public WeeklyStatisticDto weeklyStatistic(LocalDate weekStartDate, Long storeId) {
        log.info("Getting weekly statistics for week starting: {}, storeId: {}", weekStartDate, storeId);

        try {
            // Get current week's statistics
            WeeklyStatisticDto currentWeekStats = statisticRepository.getWeeklyStatistic(weekStartDate, storeId);

            // Get previous week's statistics
            WeeklyStatisticDto previousWeekStats = statisticRepository.getWeeklyStatistic(weekStartDate.minusWeeks(1), storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                currentWeekStats.getTotalSalesAmount(),
                previousWeekStats.getTotalSalesAmount()
            );

            // Return DTO with the new field
            WeeklyStatisticDto result = WeeklyStatisticDto.builder()
                    .weekStartDate(currentWeekStats.getWeekStartDate())
                    .weekEndDate(currentWeekStats.getWeekEndDate())
                    .totalSalesAmount(currentWeekStats.getTotalSalesAmount())
                    .totalOrderCount(currentWeekStats.getTotalOrderCount())
                    .totalItemCount(currentWeekStats.getTotalItemCount())
                    .averageDailySalesAmount(currentWeekStats.getAverageDailySalesAmount())
                    .uniqueCustomerCount(currentWeekStats.getUniqueCustomerCount())
                    .bestPerformingDay(currentWeekStats.getBestPerformingDay())
                    .bestDaySalesAmount(currentWeekStats.getBestDaySalesAmount())
                    .dailyBreakdown(currentWeekStats.getDailyBreakdown())
                    .changeRate(changeRate)
                    .build();

            log.info("Successfully retrieved weekly statistics for week starting: {}, storeId: {}, total sales: {}, change rate: {}%",
                    weekStartDate, storeId, result.getTotalSalesAmount(), changeRate);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving weekly statistics for week starting: {}, storeId: {}", weekStartDate, storeId, e);
            throw new RuntimeException("Failed to retrieve weekly statistics", e);
        }
    }

    @Override
    public MonthlyStatisticDto monthlyStatistic(int year, int month, Long storeId) {
        log.info("Getting monthly statistics for {}-{}, storeId: {}", year, month, storeId);

        try {
            // Get current month's statistics
            MonthlyStatisticDto currentMonthStats = statisticRepository.getMonthlyStatistic(year, month, storeId);

            // Calculate previous month's year and month
            LocalDate currentMonth = LocalDate.of(year, month, 1);
            LocalDate previousMonth = currentMonth.minusMonths(1);

            // Get previous month's statistics
            MonthlyStatisticDto previousMonthStats = statisticRepository.getMonthlyStatistic(
                previousMonth.getYear(),
                previousMonth.getMonthValue(),
                storeId
            );

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                currentMonthStats.getTotalSalesAmount(),
                previousMonthStats.getTotalSalesAmount()
            );

            // Return DTO with the new field
            MonthlyStatisticDto result = MonthlyStatisticDto.builder()
                    .month(currentMonthStats.getMonth())
                    .monthStartDate(currentMonthStats.getMonthStartDate())
                    .monthEndDate(currentMonthStats.getMonthEndDate())
                    .totalSalesAmount(currentMonthStats.getTotalSalesAmount())
                    .totalOrderCount(currentMonthStats.getTotalOrderCount())
                    .totalItemCount(currentMonthStats.getTotalItemCount())
                    .averageDailySalesAmount(currentMonthStats.getAverageDailySalesAmount())
                    .uniqueCustomerCount(currentMonthStats.getUniqueCustomerCount())
                    .changeRate(changeRate)
                    .bestPerformingWeek(currentMonthStats.getBestPerformingWeek())
                    .bestWeekSalesAmount(currentMonthStats.getBestWeekSalesAmount())
                    .topProducts(currentMonthStats.getTopProducts())
                    .weeklyBreakdown(currentMonthStats.getWeeklyBreakdown())
                    .build();

            log.info("Successfully retrieved monthly statistics for {}-{}, storeId: {}, total sales: {}, change rate: {}%",
                    year, month, storeId, result.getTotalSalesAmount(), changeRate);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving monthly statistics for {}-{}, storeId: {}", year, month, storeId, e);
            throw new RuntimeException("Failed to retrieve monthly statistics", e);
        }
    }

    /**
     * Calculate percentage change between current and previous values
     *
     * @param currentValue the current period value
     * @param previousValue the previous period value
     * @return percentage change as a double
     */
    private double calculatePercentageChange(Long currentValue, Long previousValue) {
        // Handle null values by treating them as 0
        long current = currentValue != null ? currentValue : 0L;
        long previous = previousValue != null ? previousValue : 0L;

        // Handle edge cases
        if (previous == 0) {
            // If previous period's sales are zero
            if (current > 0) {
                return 100.0; // 100% increase
            } else {
                return 0.0; // No change (both are zero)
            }
        }

        if (current == 0 && previous > 0) {
            return -100.0; // 100% decrease
        }

        // Calculate percentage change: ((current - previous) / previous) * 100
        return ((double) (current - previous) / previous) * 100.0;
    }
}
