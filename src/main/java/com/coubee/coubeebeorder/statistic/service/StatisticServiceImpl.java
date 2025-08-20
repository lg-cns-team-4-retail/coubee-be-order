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
import java.util.List;

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
            // Get current day's order aggregation data
            StatisticRepository.OrderAggregationResult todayOrderStats =
                statisticRepository.getOrderAggregation(date, date, storeId);

            // Get current day's item count
            int todayItemCount = statisticRepository.getTotalItemCount(date, date, storeId);

            // Get peak hour information
            StatisticRepository.PeakHourResult peakHour =
                statisticRepository.getPeakHour(date, storeId);

            // Calculate average order amount
            long averageOrderAmount = todayOrderStats.getTotalOrderCount() > 0
                ? todayOrderStats.getTotalSalesAmount() / todayOrderStats.getTotalOrderCount()
                : 0;

            // Get previous day's statistics for change rate calculation
            StatisticRepository.OrderAggregationResult yesterdayOrderStats =
                statisticRepository.getOrderAggregation(date.minusDays(1), date.minusDays(1), storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                todayOrderStats.getTotalSalesAmount(),
                yesterdayOrderStats.getTotalSalesAmount()
            );

            // Create result DTO
            DailyStatisticDto result = DailyStatisticDto.builder()
                    .date(date)
                    .totalSalesAmount(todayOrderStats.getTotalSalesAmount())
                    .totalOrderCount(todayOrderStats.getTotalOrderCount())
                    .totalItemCount(todayItemCount)
                    .averageOrderAmount(averageOrderAmount)
                    .uniqueCustomerCount(todayOrderStats.getUniqueCustomerCount())
                    .peakHour(peakHour.getHour())
                    .peakHourSalesAmount(peakHour.getSalesAmount())
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
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            // Get current week's order aggregation data
            StatisticRepository.OrderAggregationResult currentWeekOrderStats =
                statisticRepository.getOrderAggregation(weekStartDate, weekEndDate, storeId);

            // Get current week's item count
            int currentWeekItemCount = statisticRepository.getTotalItemCount(weekStartDate, weekEndDate, storeId);

            // Get best performing day
            StatisticRepository.BestDayResult bestDay =
                statisticRepository.getBestPerformingDay(weekStartDate, weekEndDate, storeId);

            // Get daily breakdown
            List<WeeklyStatisticDto.DailyBreakdown> dailyBreakdown =
                statisticRepository.getDailyBreakdown(weekStartDate, weekEndDate, storeId);

            // Calculate average daily sales
            long averageDailySalesAmount = currentWeekOrderStats.getTotalSalesAmount() / 7;

            // Get previous week's statistics for change rate calculation
            LocalDate previousWeekStartDate = weekStartDate.minusWeeks(1);
            LocalDate previousWeekEndDate = previousWeekStartDate.plusDays(6);
            StatisticRepository.OrderAggregationResult previousWeekOrderStats =
                statisticRepository.getOrderAggregation(previousWeekStartDate, previousWeekEndDate, storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                currentWeekOrderStats.getTotalSalesAmount(),
                previousWeekOrderStats.getTotalSalesAmount()
            );

            // Create result DTO
            WeeklyStatisticDto result = WeeklyStatisticDto.builder()
                    .weekStartDate(weekStartDate)
                    .weekEndDate(weekEndDate)
                    .totalSalesAmount(currentWeekOrderStats.getTotalSalesAmount())
                    .totalOrderCount(currentWeekOrderStats.getTotalOrderCount())
                    .totalItemCount(currentWeekItemCount)
                    .averageDailySalesAmount(averageDailySalesAmount)
                    .uniqueCustomerCount(currentWeekOrderStats.getUniqueCustomerCount())
                    .bestPerformingDay(bestDay.getDayOfWeek())
                    .bestDaySalesAmount(bestDay.getSalesAmount())
                    .dailyBreakdown(dailyBreakdown)
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
            // Calculate month start and end dates
            LocalDate monthStartDate = LocalDate.of(year, month, 1);
            LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());

            // Get current month's order aggregation data
            StatisticRepository.OrderAggregationResult currentMonthOrderStats =
                statisticRepository.getOrderAggregation(monthStartDate, monthEndDate, storeId);

            // Get current month's item count
            int currentMonthItemCount = statisticRepository.getTotalItemCount(monthStartDate, monthEndDate, storeId);

            // Get top products
            List<MonthlyStatisticDto.TopProduct> topProducts =
                statisticRepository.getTopProducts(year, month, storeId);

            // Get weekly breakdown
            List<MonthlyStatisticDto.WeeklyBreakdown> weeklyBreakdown =
                statisticRepository.getWeeklyBreakdown(year, month, storeId);

            // Calculate average daily sales
            long averageDailySalesAmount = currentMonthOrderStats.getTotalSalesAmount() / monthStartDate.lengthOfMonth();

            // Find best performing week
            String bestPerformingWeek = "No data";
            long bestWeekSalesAmount = 0L;
            if (!weeklyBreakdown.isEmpty()) {
                MonthlyStatisticDto.WeeklyBreakdown bestWeek = weeklyBreakdown.stream()
                    .max((w1, w2) -> Long.compare(w1.getSalesAmount(), w2.getSalesAmount()))
                    .orElse(null);
                if (bestWeek != null) {
                    bestPerformingWeek = bestWeek.getWeekStartDate() + " to " + bestWeek.getWeekEndDate();
                    bestWeekSalesAmount = bestWeek.getSalesAmount();
                }
            }

            // Get previous month's statistics for change rate calculation
            LocalDate previousMonth = monthStartDate.minusMonths(1);
            LocalDate previousMonthStartDate = previousMonth.withDayOfMonth(1);
            LocalDate previousMonthEndDate = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());
            StatisticRepository.OrderAggregationResult previousMonthOrderStats =
                statisticRepository.getOrderAggregation(previousMonthStartDate, previousMonthEndDate, storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                currentMonthOrderStats.getTotalSalesAmount(),
                previousMonthOrderStats.getTotalSalesAmount()
            );

            // Create result DTO
            MonthlyStatisticDto result = MonthlyStatisticDto.builder()
                    .month(String.format("%04d-%02d", year, month))
                    .monthStartDate(monthStartDate)
                    .monthEndDate(monthEndDate)
                    .totalSalesAmount(currentMonthOrderStats.getTotalSalesAmount())
                    .totalOrderCount(currentMonthOrderStats.getTotalOrderCount())
                    .totalItemCount(currentMonthItemCount)
                    .averageDailySalesAmount(averageDailySalesAmount)
                    .uniqueCustomerCount(currentMonthOrderStats.getUniqueCustomerCount())
                    .changeRate(changeRate)
                    .bestPerformingWeek(bestPerformingWeek)
                    .bestWeekSalesAmount(bestWeekSalesAmount)
                    .topProducts(topProducts)
                    .weeklyBreakdown(weeklyBreakdown)
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
