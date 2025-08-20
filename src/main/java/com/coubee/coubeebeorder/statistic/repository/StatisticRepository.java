package com.coubee.coubeebeorder.statistic.repository;

import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for sales statistics data access
 */
public interface StatisticRepository {

    /**
     * Result class for order aggregation data from orders table only
     */
    class OrderAggregationResult {
        private final Long totalSalesAmount;
        private final Integer totalOrderCount;
        private final Integer uniqueCustomerCount;

        public OrderAggregationResult(Long totalSalesAmount, Integer totalOrderCount, Integer uniqueCustomerCount) {
            this.totalSalesAmount = totalSalesAmount;
            this.totalOrderCount = totalOrderCount;
            this.uniqueCustomerCount = uniqueCustomerCount;
        }

        public Long getTotalSalesAmount() { return totalSalesAmount; }
        public Integer getTotalOrderCount() { return totalOrderCount; }
        public Integer getUniqueCustomerCount() { return uniqueCustomerCount; }
    }

    /**
     * Result class for peak hour information
     */
    class PeakHourResult {
        private final Integer hour;
        private final Long salesAmount;

        public PeakHourResult(Integer hour, Long salesAmount) {
            this.hour = hour;
            this.salesAmount = salesAmount;
        }

        public Integer getHour() { return hour; }
        public Long getSalesAmount() { return salesAmount; }
    }

    /**
     * Result class for best day information
     */
    class BestDayResult {
        private final String dayOfWeek;
        private final Long salesAmount;

        public BestDayResult(String dayOfWeek, Long salesAmount) {
            this.dayOfWeek = dayOfWeek;
            this.salesAmount = salesAmount;
        }

        public String getDayOfWeek() { return dayOfWeek; }
        public Long getSalesAmount() { return salesAmount; }
    }

    // Core aggregation methods

    /**
     * Get order aggregation data from orders table only
     *
     * @param startDate the start date for filtering
     * @param endDate the end date for filtering
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return order aggregation result
     */
    OrderAggregationResult getOrderAggregation(LocalDate startDate, LocalDate endDate, Long storeId);

    /**
     * Get total item count from order_items table using subquery to filter by relevant orders
     *
     * @param startDate the start date for filtering
     * @param endDate the end date for filtering
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return total item count
     */
    int getTotalItemCount(LocalDate startDate, LocalDate endDate, Long storeId);

    /**
     * Get peak hour information for a specific date
     *
     * @param date the date to get peak hour for
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return peak hour result
     */
    PeakHourResult getPeakHour(LocalDate date, Long storeId);

    /**
     * Get best performing day for a week
     *
     * @param weekStartDate the start date of the week
     * @param weekEndDate the end date of the week
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return best day result
     */
    BestDayResult getBestPerformingDay(LocalDate weekStartDate, LocalDate weekEndDate, Long storeId);

    /**
     * Get daily breakdown for a week
     *
     * @param weekStartDate the start date of the week
     * @param weekEndDate the end date of the week
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return list of daily breakdown
     */
    List<WeeklyStatisticDto.DailyBreakdown> getDailyBreakdown(LocalDate weekStartDate, LocalDate weekEndDate, Long storeId);

    /**
     * Get top products for a month
     *
     * @param year the year
     * @param month the month (1-12)
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return list of top products
     */
    List<MonthlyStatisticDto.TopProduct> getTopProducts(int year, int month, Long storeId);

    /**
     * Get weekly breakdown for a month
     *
     * @param year the year
     * @param month the month (1-12)
     * @param storeId the store ID to filter by (null for system-wide statistics)
     * @return list of weekly breakdown
     */
    List<MonthlyStatisticDto.WeeklyBreakdown> getWeeklyBreakdown(int year, int month, Long storeId);
}
