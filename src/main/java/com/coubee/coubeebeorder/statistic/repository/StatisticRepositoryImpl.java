package com.coubee.coubeebeorder.statistic.repository;

import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * JdbcTemplate-based implementation for sales statistics data access
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StatisticRepositoryImpl implements StatisticRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public DailyStatisticDto getDailyStatistic(LocalDate date) {
        log.info("Getting daily statistics for date: {}", date);

        // Convert LocalDate to UNIX timestamps for the day range
        long startUnix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Query for basic daily statistics
        String sql = """
            SELECT 
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.id) as total_order_count,
                COALESCE(SUM(oi.quantity), 0) as total_item_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ? 
            AND o.paid_at_unix BETWEEN ? AND ?
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSalesAmount = rs.getLong("total_sales_amount");
            int totalOrderCount = rs.getInt("total_order_count");
            int totalItemCount = rs.getInt("total_item_count");
            int uniqueCustomerCount = rs.getInt("unique_customer_count");

            // Calculate average order amount
            long averageOrderAmount = totalOrderCount > 0 ? totalSalesAmount / totalOrderCount : 0;

            // Get peak hour information
            PeakHourInfo peakHourInfo = getPeakHourInfo(date);

            return DailyStatisticDto.builder()
                    .date(date)
                    .totalSalesAmount(totalSalesAmount)
                    .totalOrderCount(totalOrderCount)
                    .totalItemCount(totalItemCount)
                    .averageOrderAmount(averageOrderAmount)
                    .uniqueCustomerCount(uniqueCustomerCount)
                    .peakHour(peakHourInfo.hour)
                    .peakHourSalesAmount(peakHourInfo.salesAmount)
                    .build();
        }, OrderStatus.RECEIVED.name(), startUnix, endUnix);
    }

    @Override
    public WeeklyStatisticDto getWeeklyStatistic(LocalDate weekStartDate) {
        log.info("Getting weekly statistics for week starting: {}", weekStartDate);

        LocalDate weekEndDate = weekStartDate.plusDays(6);
        long startUnix = weekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = weekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Query for basic weekly statistics
        String sql = """
            SELECT 
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.id) as total_order_count,
                COALESCE(SUM(oi.quantity), 0) as total_item_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ? 
            AND o.paid_at_unix BETWEEN ? AND ?
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSalesAmount = rs.getLong("total_sales_amount");
            int totalOrderCount = rs.getInt("total_order_count");
            int totalItemCount = rs.getInt("total_item_count");
            int uniqueCustomerCount = rs.getInt("unique_customer_count");

            // Calculate average daily sales
            long averageDailySalesAmount = totalSalesAmount / 7;

            // Get best performing day
            BestDayInfo bestDayInfo = getBestPerformingDay(weekStartDate, weekEndDate);

            // Get daily breakdown
            List<WeeklyStatisticDto.DailyBreakdown> dailyBreakdown = getDailyBreakdown(weekStartDate, weekEndDate);

            return WeeklyStatisticDto.builder()
                    .weekStartDate(weekStartDate)
                    .weekEndDate(weekEndDate)
                    .totalSalesAmount(totalSalesAmount)
                    .totalOrderCount(totalOrderCount)
                    .totalItemCount(totalItemCount)
                    .averageDailySalesAmount(averageDailySalesAmount)
                    .uniqueCustomerCount(uniqueCustomerCount)
                    .bestPerformingDay(bestDayInfo.dayOfWeek)
                    .bestDaySalesAmount(bestDayInfo.salesAmount)
                    .dailyBreakdown(dailyBreakdown)
                    .build();
        }, OrderStatus.RECEIVED.name(), startUnix, endUnix);
    }

    @Override
    public MonthlyStatisticDto getMonthlyStatistic(int year, int month) {
        log.info("Getting monthly statistics for {}-{}", year, month);

        LocalDate monthStartDate = LocalDate.of(year, month, 1);
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
        long startUnix = monthStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = monthEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Query for basic monthly statistics
        String sql = """
            SELECT 
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.id) as total_order_count,
                COALESCE(SUM(oi.quantity), 0) as total_item_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ? 
            AND o.paid_at_unix BETWEEN ? AND ?
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSalesAmount = rs.getLong("total_sales_amount");
            int totalOrderCount = rs.getInt("total_order_count");
            int totalItemCount = rs.getInt("total_item_count");
            int uniqueCustomerCount = rs.getInt("unique_customer_count");

            // Calculate average daily sales
            long averageDailySalesAmount = totalSalesAmount / monthStartDate.lengthOfMonth();

            // Calculate growth rate (placeholder - would need previous month data)
            double growthRate = 0.0; // TODO: Implement growth rate calculation

            // Get best performing week (placeholder)
            String bestPerformingWeek = "Week 1"; // TODO: Implement best week calculation
            long bestWeekSalesAmount = 0L; // TODO: Implement best week sales calculation

            // Get top products and weekly breakdown (placeholder)
            List<MonthlyStatisticDto.TopProduct> topProducts = new ArrayList<>();
            List<MonthlyStatisticDto.WeeklyBreakdown> weeklyBreakdown = new ArrayList<>();

            return MonthlyStatisticDto.builder()
                    .month(String.format("%04d-%02d", year, month))
                    .monthStartDate(monthStartDate)
                    .monthEndDate(monthEndDate)
                    .totalSalesAmount(totalSalesAmount)
                    .totalOrderCount(totalOrderCount)
                    .totalItemCount(totalItemCount)
                    .averageDailySalesAmount(averageDailySalesAmount)
                    .uniqueCustomerCount(uniqueCustomerCount)
                    .growthRate(growthRate)
                    .bestPerformingWeek(bestPerformingWeek)
                    .bestWeekSalesAmount(bestWeekSalesAmount)
                    .topProducts(topProducts)
                    .weeklyBreakdown(weeklyBreakdown)
                    .build();
        }, OrderStatus.RECEIVED.name(), startUnix, endUnix);
    }

    private PeakHourInfo getPeakHourInfo(LocalDate date) {
        // Placeholder implementation - would need to extract hour from timestamp and group by hour
        return new PeakHourInfo(14, 25000L); // Default peak hour at 2 PM
    }

    private BestDayInfo getBestPerformingDay(LocalDate weekStartDate, LocalDate weekEndDate) {
        // Placeholder implementation - would need to group by day and find max
        return new BestDayInfo("FRIDAY", 200000L);
    }

    private List<WeeklyStatisticDto.DailyBreakdown> getDailyBreakdown(LocalDate weekStartDate, LocalDate weekEndDate) {
        // Placeholder implementation - would need to group by day
        return new ArrayList<>();
    }

    // Helper classes for internal data transfer
    private static class PeakHourInfo {
        final Integer hour;
        final Long salesAmount;

        PeakHourInfo(Integer hour, Long salesAmount) {
            this.hour = hour;
            this.salesAmount = salesAmount;
        }
    }

    private static class BestDayInfo {
        final String dayOfWeek;
        final Long salesAmount;

        BestDayInfo(String dayOfWeek, Long salesAmount) {
            this.dayOfWeek = dayOfWeek;
            this.salesAmount = salesAmount;
        }
    }
}
