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
    public DailyStatisticDto getDailyStatistic(LocalDate date, Long storeId) {
        log.info("Getting daily statistics for date: {}, storeId: {}", date, storeId);

        // Convert LocalDate to UNIX timestamps for the day range
        long startUnix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.id) as total_order_count,
                COALESCE(SUM(oi.quantity), 0) as total_item_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSalesAmount = rs.getLong("total_sales_amount");
            int totalOrderCount = rs.getInt("total_order_count");
            int totalItemCount = rs.getInt("total_item_count");
            int uniqueCustomerCount = rs.getInt("unique_customer_count");

            // Calculate average order amount
            long averageOrderAmount = totalOrderCount > 0 ? totalSalesAmount / totalOrderCount : 0;

            // Get peak hour information
            PeakHourInfo peakHourInfo = getPeakHourInfo(date, storeId);

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
        }, params);
    }

    @Override
    public WeeklyStatisticDto getWeeklyStatistic(LocalDate weekStartDate, Long storeId) {
        log.info("Getting weekly statistics for week starting: {}, storeId: {}", weekStartDate, storeId);

        LocalDate weekEndDate = weekStartDate.plusDays(6);
        long startUnix = weekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = weekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.id) as total_order_count,
                COALESCE(SUM(oi.quantity), 0) as total_item_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSalesAmount = rs.getLong("total_sales_amount");
            int totalOrderCount = rs.getInt("total_order_count");
            int totalItemCount = rs.getInt("total_item_count");
            int uniqueCustomerCount = rs.getInt("unique_customer_count");

            // Calculate average daily sales
            long averageDailySalesAmount = totalSalesAmount / 7;

            // Get best performing day
            BestDayInfo bestDayInfo = getBestPerformingDay(weekStartDate, weekEndDate, storeId);

            // Get daily breakdown
            List<WeeklyStatisticDto.DailyBreakdown> dailyBreakdown = getDailyBreakdown(weekStartDate, weekEndDate, storeId);

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
        }, params);
    }

    @Override
    public MonthlyStatisticDto getMonthlyStatistic(int year, int month, Long storeId) {
        log.info("Getting monthly statistics for {}-{}, storeId: {}", year, month, storeId);

        LocalDate monthStartDate = LocalDate.of(year, month, 1);
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
        long startUnix = monthStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = monthEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                COALESCE(SUM(o.total_amount), 0) as total_sales_amount,
                COUNT(o.id) as total_order_count,
                COALESCE(SUM(oi.quantity), 0) as total_item_count,
                COUNT(DISTINCT o.user_id) as unique_customer_count
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSalesAmount = rs.getLong("total_sales_amount");
            int totalOrderCount = rs.getInt("total_order_count");
            int totalItemCount = rs.getInt("total_item_count");
            int uniqueCustomerCount = rs.getInt("unique_customer_count");

            // Calculate average daily sales
            long averageDailySalesAmount = totalSalesAmount / monthStartDate.lengthOfMonth();

            // Calculate change rate (this is now handled in the service layer)
            double changeRate = 0.0; // Will be set by service layer

            // Get best performing week from weekly breakdown
            String bestPerformingWeek = "Week 1"; // Default value
            long bestWeekSalesAmount = 0L; // Default value

            // Find best performing week from weekly breakdown
            if (!weeklyBreakdown.isEmpty()) {
                MonthlyStatisticDto.WeeklyBreakdown bestWeek = weeklyBreakdown.stream()
                        .max((w1, w2) -> Long.compare(w1.getSalesAmount(), w2.getSalesAmount()))
                        .orElse(null);

                if (bestWeek != null) {
                    bestPerformingWeek = String.format("%s to %s",
                            bestWeek.getWeekStartDate(),
                            bestWeek.getWeekEndDate());
                    bestWeekSalesAmount = bestWeek.getSalesAmount();
                }
            }

            // Get top products and weekly breakdown
            List<MonthlyStatisticDto.TopProduct> topProducts = getTopProducts(year, month, storeId);
            List<MonthlyStatisticDto.WeeklyBreakdown> weeklyBreakdown = getWeeklyBreakdown(year, month, storeId);

            return MonthlyStatisticDto.builder()
                    .month(String.format("%04d-%02d", year, month))
                    .monthStartDate(monthStartDate)
                    .monthEndDate(monthEndDate)
                    .totalSalesAmount(totalSalesAmount)
                    .totalOrderCount(totalOrderCount)
                    .totalItemCount(totalItemCount)
                    .averageDailySalesAmount(averageDailySalesAmount)
                    .uniqueCustomerCount(uniqueCustomerCount)
                    .changeRate(changeRate)
                    .bestPerformingWeek(bestPerformingWeek)
                    .bestWeekSalesAmount(bestWeekSalesAmount)
                    .topProducts(topProducts)
                    .weeklyBreakdown(weeklyBreakdown)
                    .build();
        }, params);
    }

    private PeakHourInfo getPeakHourInfo(LocalDate date, Long storeId) {
        log.debug("Getting peak hour info for date: {}, storeId: {}", date, storeId);

        // Convert LocalDate to UNIX timestamps for the day range
        long startUnix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                EXTRACT(HOUR FROM TO_TIMESTAMP(o.paid_at_unix)) as hour,
                COALESCE(SUM(o.total_amount), 0) as hourly_sales
            FROM orders o
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        sqlBuilder.append("""
            GROUP BY EXTRACT(HOUR FROM TO_TIMESTAMP(o.paid_at_unix))
            ORDER BY hourly_sales DESC
            LIMIT 1
            """);

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                int hour = rs.getInt("hour");
                long salesAmount = rs.getLong("hourly_sales");
                return new PeakHourInfo(hour, salesAmount);
            }, params);
        } catch (Exception e) {
            log.warn("No peak hour data found for date: {}, storeId: {}, returning default", date, storeId);
            // Return default peak hour if no data found
            return new PeakHourInfo(14, 0L);
        }
    }

    private BestDayInfo getBestPerformingDay(LocalDate weekStartDate, LocalDate weekEndDate, Long storeId) {
        log.debug("Getting best performing day for week: {} to {}, storeId: {}", weekStartDate, weekEndDate, storeId);

        // Convert LocalDate to UNIX timestamps for the week range
        long startUnix = weekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = weekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY') as day_name,
                COALESCE(SUM(o.total_amount), 0) as daily_sales
            FROM orders o
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        sqlBuilder.append("""
            GROUP BY TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY')
            ORDER BY daily_sales DESC
            LIMIT 1
            """);

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String dayName = rs.getString("day_name").trim(); // PostgreSQL pads day names
                long salesAmount = rs.getLong("daily_sales");
                return new BestDayInfo(dayName, salesAmount);
            }, params);
        } catch (Exception e) {
            log.warn("No best day data found for week: {} to {}, storeId: {}, returning default", weekStartDate, weekEndDate, storeId);
            // Return default best day if no data found
            return new BestDayInfo("FRIDAY", 0L);
        }
    }

    private List<WeeklyStatisticDto.DailyBreakdown> getDailyBreakdown(LocalDate weekStartDate, LocalDate weekEndDate, Long storeId) {
        log.debug("Getting daily breakdown for week: {} to {}, storeId: {}", weekStartDate, weekEndDate, storeId);

        // Convert LocalDate to UNIX timestamps for the week range
        long startUnix = weekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = weekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY') as day_of_week,
                DATE(TO_TIMESTAMP(o.paid_at_unix)) as order_date,
                COALESCE(SUM(o.total_amount), 0) as sales_amount,
                COUNT(o.id) as order_count
            FROM orders o
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        sqlBuilder.append("""
            GROUP BY TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY'), DATE(TO_TIMESTAMP(o.paid_at_unix))
            ORDER BY order_date
            """);

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String dayOfWeek = rs.getString("day_of_week").trim(); // PostgreSQL pads day names
                LocalDate date = rs.getDate("order_date").toLocalDate();
                long salesAmount = rs.getLong("sales_amount");
                int orderCount = rs.getInt("order_count");

                return WeeklyStatisticDto.DailyBreakdown.builder()
                        .dayOfWeek(dayOfWeek)
                        .date(date)
                        .salesAmount(salesAmount)
                        .orderCount(orderCount)
                        .build();
            }, params);
        } catch (Exception e) {
            log.warn("No daily breakdown data found for week: {} to {}, storeId: {}, returning empty list", weekStartDate, weekEndDate, storeId);
            return new ArrayList<>();
        }
    }

    private List<MonthlyStatisticDto.TopProduct> getTopProducts(int year, int month, Long storeId) {
        log.debug("Getting top products for {}-{}, storeId: {}", year, month, storeId);

        LocalDate monthStartDate = LocalDate.of(year, month, 1);
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
        long startUnix = monthStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = monthEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                oi.product_id,
                oi.product_name,
                SUM(oi.quantity) as quantity_sold,
                SUM(oi.quantity * oi.price) as sales_amount
            FROM orders o
            INNER JOIN order_items oi ON o.id = oi.order_id
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            AND oi.event_type = 'PURCHASE'
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        sqlBuilder.append("""
            GROUP BY oi.product_id, oi.product_name
            ORDER BY sales_amount DESC
            LIMIT 5
            """);

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Long productId = rs.getLong("product_id");
                String productName = rs.getString("product_name");
                int quantitySold = rs.getInt("quantity_sold");
                long salesAmount = rs.getLong("sales_amount");

                return MonthlyStatisticDto.TopProduct.builder()
                        .productId(productId)
                        .productName(productName)
                        .quantitySold(quantitySold)
                        .salesAmount(salesAmount)
                        .build();
            }, params);
        } catch (Exception e) {
            log.warn("No top products data found for {}-{}, storeId: {}, returning empty list", year, month, storeId);
            return new ArrayList<>();
        }
    }

    private List<MonthlyStatisticDto.WeeklyBreakdown> getWeeklyBreakdown(int year, int month, Long storeId) {
        log.debug("Getting weekly breakdown for {}-{}, storeId: {}", year, month, storeId);

        LocalDate monthStartDate = LocalDate.of(year, month, 1);
        LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
        long startUnix = monthStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long endUnix = monthEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

        // Build SQL query with conditional store filtering
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                EXTRACT(WEEK FROM TO_TIMESTAMP(o.paid_at_unix)) - EXTRACT(WEEK FROM DATE_TRUNC('month', TO_TIMESTAMP(o.paid_at_unix))) + 1 as week_number,
                DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix))::date as week_start_date,
                (DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix)) + INTERVAL '6 days')::date as week_end_date,
                COALESCE(SUM(o.total_amount), 0) as sales_amount,
                COUNT(o.id) as order_count
            FROM orders o
            WHERE o.status = ?
            AND o.paid_at_unix BETWEEN ? AND ?
            """);

        // Add store filter if storeId is provided
        if (storeId != null) {
            sqlBuilder.append("AND o.store_id = ? ");
        }

        sqlBuilder.append("""
            GROUP BY
                EXTRACT(WEEK FROM TO_TIMESTAMP(o.paid_at_unix)) - EXTRACT(WEEK FROM DATE_TRUNC('month', TO_TIMESTAMP(o.paid_at_unix))) + 1,
                DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix))
            ORDER BY week_start_date
            """);

        String sql = sqlBuilder.toString();

        // Prepare parameters based on whether storeId is provided
        Object[] params;
        if (storeId != null) {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix, storeId};
        } else {
            params = new Object[]{OrderStatus.RECEIVED.name(), startUnix, endUnix};
        }

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                int weekNumber = rs.getInt("week_number");
                LocalDate weekStartDate = rs.getDate("week_start_date").toLocalDate();
                LocalDate weekEndDate = rs.getDate("week_end_date").toLocalDate();
                long salesAmount = rs.getLong("sales_amount");
                int orderCount = rs.getInt("order_count");

                return MonthlyStatisticDto.WeeklyBreakdown.builder()
                        .weekNumber(weekNumber)
                        .weekStartDate(weekStartDate)
                        .weekEndDate(weekEndDate)
                        .salesAmount(salesAmount)
                        .orderCount(orderCount)
                        .build();
            }, params);
        } catch (Exception e) {
            log.warn("No weekly breakdown data found for {}-{}, storeId: {}, returning empty list", year, month, storeId);
            return new ArrayList<>();
        }
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
