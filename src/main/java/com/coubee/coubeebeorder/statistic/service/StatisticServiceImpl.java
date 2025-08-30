package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.dto.ProductSalesSummaryDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.projection.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for sales statistics business logic
 * Refactored to use JPA OrderRepository instead of JdbcTemplate StatisticRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticServiceImpl implements StatisticService {

    private final OrderRepository orderRepository;
    private final StoreClient storeClient;

    /**
     * Validate store access for the given user
     * Checks if the user owns the store and if the store is approved
     */
    private void validateStoreAccess(Long userId, Long storeId) {
        if (storeId == null) {
            return; // System-wide statistics don't require store validation
        }

        try {
            // Get user's owned approved stores from store service
            ApiResponseDto<List<Long>> ownedStoresResponse = storeClient.getStoresByOwnerIdOnApproved(userId);
            List<Long> ownedStoreIds = ownedStoresResponse.getData();

            if (ownedStoreIds == null || !ownedStoreIds.contains(storeId)) {
                throw new ApiError("Access denied: You do not own this store or it is not approved");
            }

            // Store status validation temporarily disabled for production compatibility
            // The isStoreApproved API is not yet available in the production main branch
            /*
            ApiResponseDto<Boolean> storeStatusResponse = storeClient.isStoreApproved(storeId);
            Boolean isApproved = storeStatusResponse.getData();

            if (isApproved == null || !isApproved) {
                throw new ApiError("Access denied: Store is not approved");
            }
            */
        } catch (Exception e) {
            if (e instanceof ApiError) {
                throw e;
            }
            log.error("Error validating store access for userId: {}, storeId: {}", userId, storeId, e);
            throw new ApiError("Unable to validate store access");
        }
    }

    @Override
    public DailyStatisticDto dailyStatistic(LocalDate date, Long storeId, Long userId) {
        log.info("Getting daily statistics for date: {}, storeId: {}, userId: {}", date, storeId, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // Convert LocalDate to UNIX timestamps for today
            long todayStartUnix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long todayEndUnix = date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get current day's order aggregation data
            OrderAggregationProjection todayOrderStats =
                orderRepository.getOrderAggregation(todayStartUnix, todayEndUnix, storeId);

            // Get current day's item count
            TotalItemCountProjection todayItemCountProjection =
                orderRepository.getTotalItemCount(todayStartUnix, todayEndUnix, storeId);
            int todayItemCount = todayItemCountProjection.getTotalItemCount();

            // Get peak hour information with null-safe handling
            PeakHourProjection peakHour = orderRepository.getPeakHour(todayStartUnix, todayEndUnix, storeId)
                .orElse(createDefaultPeakHourProjection());

            // Calculate average order amount
            long averageOrderAmount = todayOrderStats.getTotalOrderCount() > 0
                ? todayOrderStats.getTotalSalesAmount() / todayOrderStats.getTotalOrderCount()
                : 0;

            // Convert LocalDate to UNIX timestamps for yesterday
            LocalDate yesterday = date.minusDays(1);
            long yesterdayStartUnix = yesterday.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long yesterdayEndUnix = yesterday.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get previous day's statistics for change rate calculation
            OrderAggregationProjection yesterdayOrderStats =
                orderRepository.getOrderAggregation(yesterdayStartUnix, yesterdayEndUnix, storeId);

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
                    .peakHourSalesAmount(peakHour.getHourlySales())
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
    public WeeklyStatisticDto weeklyStatistic(LocalDate weekStartDate, Long storeId, Long userId) {
        log.info("Getting weekly statistics for week starting: {}, storeId: {}, userId: {}", weekStartDate, storeId, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            // Convert LocalDate to UNIX timestamps for current week
            long currentWeekStartUnix = weekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long currentWeekEndUnix = weekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get current week's order aggregation data
            OrderAggregationProjection currentWeekOrderStats =
                orderRepository.getOrderAggregation(currentWeekStartUnix, currentWeekEndUnix, storeId);

            // Get current week's item count
            TotalItemCountProjection currentWeekItemCountProjection =
                orderRepository.getTotalItemCount(currentWeekStartUnix, currentWeekEndUnix, storeId);
            int currentWeekItemCount = currentWeekItemCountProjection.getTotalItemCount();

            // Get best performing day with null-safe handling
            BestDayProjection bestDay = orderRepository.getBestPerformingDay(currentWeekStartUnix, currentWeekEndUnix, storeId)
                .orElse(createDefaultBestDayProjection());

            // Get daily breakdown
            List<DailyBreakdownProjection> dailyBreakdownProjections =
                orderRepository.getDailyBreakdown(currentWeekStartUnix, currentWeekEndUnix, storeId);

            // Convert projections to DTOs
            List<WeeklyStatisticDto.DailyBreakdown> dailyBreakdown = dailyBreakdownProjections.stream()
                .map(projection -> WeeklyStatisticDto.DailyBreakdown.builder()
                    .dayOfWeek(projection.getDayOfWeek().trim()) // Trim whitespace from PostgreSQL TO_CHAR
                    .date(projection.getOrderDate())
                    .salesAmount(projection.getSalesAmount())
                    .orderCount(projection.getOrderCount())
                    .build())
                .collect(Collectors.toList());

            // Calculate average daily sales
            long averageDailySalesAmount = currentWeekOrderStats.getTotalSalesAmount() / 7;

            // Convert LocalDate to UNIX timestamps for previous week
            LocalDate previousWeekStartDate = weekStartDate.minusWeeks(1);
            LocalDate previousWeekEndDate = previousWeekStartDate.plusDays(6);
            long previousWeekStartUnix = previousWeekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long previousWeekEndUnix = previousWeekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get previous week's statistics for change rate calculation
            OrderAggregationProjection previousWeekOrderStats =
                orderRepository.getOrderAggregation(previousWeekStartUnix, previousWeekEndUnix, storeId);

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
                    .bestPerformingDay(bestDay.getDayName().trim())
                    .bestDaySalesAmount(bestDay.getDailySales())
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
    public MonthlyStatisticDto monthlyStatistic(int year, int month, Long storeId, Long userId) {
        log.info("Getting monthly statistics for {}-{}, storeId: {}, userId: {}", year, month, storeId, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // Calculate month start and end dates
            LocalDate monthStartDate = LocalDate.of(year, month, 1);
            LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());

            // Convert LocalDate to UNIX timestamps for current month
            long currentMonthStartUnix = monthStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long currentMonthEndUnix = monthEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get current month's order aggregation data
            OrderAggregationProjection currentMonthOrderStats =
                orderRepository.getOrderAggregation(currentMonthStartUnix, currentMonthEndUnix, storeId);

            // Get current month's item count
            TotalItemCountProjection currentMonthItemCountProjection =
                orderRepository.getTotalItemCount(currentMonthStartUnix, currentMonthEndUnix, storeId);
            int currentMonthItemCount = currentMonthItemCountProjection.getTotalItemCount();

            // Get top products
            List<TopProductProjection> topProductProjections =
                orderRepository.getTopProducts(currentMonthStartUnix, currentMonthEndUnix, storeId);

            // Convert projections to DTOs
            List<MonthlyStatisticDto.TopProduct> topProducts = topProductProjections.stream()
                .map(projection -> MonthlyStatisticDto.TopProduct.builder()
                    .productId(projection.getProductId())
                    .productName(projection.getProductName())
                    .quantitySold(projection.getQuantitySold())
                    .salesAmount(projection.getSalesAmount())
                    .build())
                .collect(Collectors.toList());

            // Get weekly breakdown
            List<WeeklyBreakdownProjection> weeklyBreakdownProjections =
                orderRepository.getWeeklyBreakdown(currentMonthStartUnix, currentMonthEndUnix, storeId);

            // Convert projections to DTOs
            List<MonthlyStatisticDto.WeeklyBreakdown> weeklyBreakdown = weeklyBreakdownProjections.stream()
                .map(projection -> MonthlyStatisticDto.WeeklyBreakdown.builder()
                    .weekNumber(projection.getWeekNumber())
                    .weekStartDate(projection.getWeekStartDate())
                    .weekEndDate(projection.getWeekEndDate())
                    .salesAmount(projection.getSalesAmount())
                    .orderCount(projection.getOrderCount())
                    .build())
                .collect(Collectors.toList());

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

            // Convert LocalDate to UNIX timestamps for previous month
            LocalDate previousMonth = monthStartDate.minusMonths(1);
            LocalDate previousMonthStartDate = previousMonth.withDayOfMonth(1);
            LocalDate previousMonthEndDate = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());
            long previousMonthStartUnix = previousMonthStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long previousMonthEndUnix = previousMonthEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get previous month's statistics for change rate calculation
            OrderAggregationProjection previousMonthOrderStats =
                orderRepository.getOrderAggregation(previousMonthStartUnix, previousMonthEndUnix, storeId);

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

    /**
     * Create a default PeakHourProjection for null-safe handling
     * @return default projection with null hour and 0 sales
     */
    private PeakHourProjection createDefaultPeakHourProjection() {
        return new PeakHourProjection() {
            @Override
            public Integer getHour() {
                return null; // No peak hour data available
            }

            @Override
            public Long getHourlySales() {
                return 0L; // No sales data available
            }
        };
    }

    /**
     * Create a default BestDayProjection for null-safe handling
     * @return default projection with "No data" day name and 0 sales
     */
    private BestDayProjection createDefaultBestDayProjection() {
        return new BestDayProjection() {
            @Override
            public String getDayName() {
                return "No data"; // No best day data available
            }

            @Override
            public Long getDailySales() {
                return 0L; // No sales data available
            }
        };
    }

    @Override
    public List<ProductSalesSummaryDto> getProductSalesSummary(Long storeId, LocalDate startDate, LocalDate endDate, Long userId) {
        log.info("Getting product sales summary for storeId: {}, startDate: {}, endDate: {}, userId: {}",
                storeId, startDate, endDate, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // Convert LocalDate to UNIX timestamps
            long startUnix = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long endUnix = endDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get product sales summary from repository
            List<OrderRepository.ProductSalesSummaryProjection> projections =
                orderRepository.findProductSalesSummaryByStore(storeId, startUnix, endUnix);

            // Convert projections to DTOs
            List<ProductSalesSummaryDto> result = projections.stream()
                .map(projection -> ProductSalesSummaryDto.builder()
                    .productId(projection.getProductId())
                    .productName(projection.getProductName())
                    .quantitySold(projection.getQuantitySold())
                    .totalSalesAmount(projection.getTotalSalesAmount())
                    .build())
                .collect(Collectors.toList());

            log.info("Successfully retrieved product sales summary for storeId: {}, found {} products",
                    storeId, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error retrieving product sales summary for storeId: {}, startDate: {}, endDate: {}",
                    storeId, startDate, endDate, e);
            throw new RuntimeException("Failed to retrieve product sales summary", e);
        }
    }

    // ========================================
    // Enhanced Statistics Methods with Hotdeal Support
    // ========================================

    @Override
    public DailyStatisticResponseDto getDailyStatisticsWithHotdeal(LocalDate date, Long storeId, Long userId) {
        log.info("Getting comprehensive daily statistics with hotdeal for date: {}, storeId: {}, userId: {}", date, storeId, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // Convert LocalDate to UNIX timestamps for today
            long todayStartUnix = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long todayEndUnix = date.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get comprehensive statistics
            OrderRepository.ComprehensiveStatsProjection stats =
                orderRepository.getComprehensiveStatistics(todayStartUnix, todayEndUnix, storeId);

            // Get hourly breakdown
            List<OrderRepository.HourlyBreakdownProjection> hourlyData =
                orderRepository.getHourlyBreakdownWithHotdeal(todayStartUnix, todayEndUnix, storeId);

            // Get sold items summary
            List<OrderRepository.SoldItemsSummaryProjection> soldItemsData =
                orderRepository.getSoldItemsSummaryWithHotdeal(todayStartUnix, todayEndUnix, storeId);

            // Calculate previous day for change rate
            LocalDate previousDate = date.minusDays(1);
            long previousStartUnix = previousDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long previousEndUnix = previousDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
            OrderRepository.ComprehensiveStatsProjection previousStats =
                orderRepository.getComprehensiveStatistics(previousStartUnix, previousEndUnix, storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                stats.getTotalSalesAmount(),
                previousStats.getTotalSalesAmount()
            );

            // Build response DTO
            return buildDailyStatisticResponseDto(date, stats, hourlyData, soldItemsData, changeRate);

        } catch (Exception e) {
            log.error("Error retrieving comprehensive daily statistics for date: {}, storeId: {}", date, storeId, e);
            throw new RuntimeException("Failed to retrieve comprehensive daily statistics", e);
        }
    }

    @Override
    public WeeklyStatisticResponseDto getWeeklyStatisticsWithHotdeal(LocalDate weekStartDate, Long storeId, Long userId) {
        log.info("Getting comprehensive weekly statistics with hotdeal for week starting: {}, storeId: {}, userId: {}", weekStartDate, storeId, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // Calculate week end date
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            // Convert to UNIX timestamps
            long currentWeekStartUnix = weekStartDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long currentWeekEndUnix = weekEndDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get comprehensive statistics
            OrderRepository.ComprehensiveStatsProjection stats =
                orderRepository.getComprehensiveStatistics(currentWeekStartUnix, currentWeekEndUnix, storeId);

            // Get daily breakdown
            List<OrderRepository.DailyBreakdownWithHotdealProjection> dailyData =
                orderRepository.getDailyBreakdownWithHotdeal(currentWeekStartUnix, currentWeekEndUnix, storeId);

            // Get sold items summary
            List<OrderRepository.SoldItemsSummaryProjection> soldItemsData =
                orderRepository.getSoldItemsSummaryWithHotdeal(currentWeekStartUnix, currentWeekEndUnix, storeId);

            // Calculate previous week for change rate
            LocalDate previousWeekStart = weekStartDate.minusWeeks(1);
            LocalDate previousWeekEnd = previousWeekStart.plusDays(6);
            long previousWeekStartUnix = previousWeekStart.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long previousWeekEndUnix = previousWeekEnd.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
            OrderRepository.ComprehensiveStatsProjection previousStats =
                orderRepository.getComprehensiveStatistics(previousWeekStartUnix, previousWeekEndUnix, storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                stats.getTotalSalesAmount(),
                previousStats.getTotalSalesAmount()
            );

            // Build response DTO
            return buildWeeklyStatisticResponseDto(weekStartDate, weekEndDate, stats, dailyData, soldItemsData, changeRate);

        } catch (Exception e) {
            log.error("Error retrieving comprehensive weekly statistics for week starting: {}, storeId: {}", weekStartDate, storeId, e);
            throw new RuntimeException("Failed to retrieve comprehensive weekly statistics", e);
        }
    }

    @Override
    public MonthlyStatisticResponseDto getMonthlyStatisticsWithHotdeal(int year, int month, Long storeId, Long userId) {
        log.info("Getting comprehensive monthly statistics with hotdeal for {}-{}, storeId: {}, userId: {}", year, month, storeId, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // Calculate month boundaries
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            // Convert to UNIX timestamps
            long currentMonthStartUnix = monthStart.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long currentMonthEndUnix = monthEnd.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // Get comprehensive statistics
            OrderRepository.ComprehensiveStatsProjection stats =
                orderRepository.getComprehensiveStatistics(currentMonthStartUnix, currentMonthEndUnix, storeId);

            // Get weekly breakdown
            List<OrderRepository.WeeklyBreakdownWithHotdealProjection> weeklyData =
                orderRepository.getWeeklyBreakdownWithHotdeal(currentMonthStartUnix, currentMonthEndUnix, storeId);

            // Get sold items summary
            List<OrderRepository.SoldItemsSummaryProjection> soldItemsData =
                orderRepository.getSoldItemsSummaryWithHotdeal(currentMonthStartUnix, currentMonthEndUnix, storeId);

            // Calculate previous month for change rate
            LocalDate previousMonthStart = monthStart.minusMonths(1);
            LocalDate previousMonthEnd = previousMonthStart.withDayOfMonth(previousMonthStart.lengthOfMonth());
            long previousMonthStartUnix = previousMonthStart.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long previousMonthEndUnix = previousMonthEnd.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
            OrderRepository.ComprehensiveStatsProjection previousStats =
                orderRepository.getComprehensiveStatistics(previousMonthStartUnix, previousMonthEndUnix, storeId);

            // Calculate change rate
            double changeRate = calculatePercentageChange(
                stats.getTotalSalesAmount(),
                previousStats.getTotalSalesAmount()
            );

            // Build response DTO
            return buildMonthlyStatisticResponseDto(year, month, monthStart, monthEnd, stats, weeklyData, soldItemsData, changeRate);

        } catch (Exception e) {
            log.error("Error retrieving comprehensive monthly statistics for {}-{}, storeId: {}", year, month, storeId, e);
            throw new RuntimeException("Failed to retrieve comprehensive monthly statistics", e);
        }
    }

    // ========================================
    // Helper Methods for Building Response DTOs
    // ========================================

    private DailyStatisticResponseDto buildDailyStatisticResponseDto(
            LocalDate date,
            OrderRepository.ComprehensiveStatsProjection stats,
            List<OrderRepository.HourlyBreakdownProjection> hourlyData,
            List<OrderRepository.SoldItemsSummaryProjection> soldItemsData,
            double changeRate) {

        // Build overall summary
        DailyStatisticResponseDto.OverallSummary overallSummary = DailyStatisticResponseDto.OverallSummary.builder()
                .totalSalesAmount(stats.getTotalSalesAmount())
                .totalOrderCount(stats.getTotalOrderCount())
                .totalItemCount(stats.getTotalItemCount())
                .averageOrderAmount(stats.getTotalOrderCount() > 0 ? stats.getTotalSalesAmount() / stats.getTotalOrderCount() : 0L)
                .uniqueCustomerCount(stats.getUniqueCustomerCount())
                .peakHour(findPeakHour(hourlyData))
                .peakHourSalesAmount(findPeakHourSales(hourlyData))
                .build();

        // Build hotdeal analysis
        DailyStatisticResponseDto.HotdealAnalysis hotdealAnalysis = buildHotdealAnalysis(stats);

        // Build hourly breakdown
        List<DailyStatisticResponseDto.HourlyBreakdown> hourlyBreakdown = hourlyData.stream()
                .map(h -> DailyStatisticResponseDto.HourlyBreakdown.builder()
                        .hour(h.getHour())
                        .salesAmount(h.getSalesAmount())
                        .orderCount(h.getOrderCount())
                        .hotdealSalesAmount(h.getHotdealSalesAmount())
                        .regularSalesAmount(h.getRegularSalesAmount())
                        .build())
                .collect(Collectors.toList());

        // Build sold items summary
        List<DailyStatisticResponseDto.SoldItemSummary> soldItemsSummary = soldItemsData.stream()
                .map(item -> DailyStatisticResponseDto.SoldItemSummary.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .totalQuantity(item.getTotalQuantity())
                        .totalRevenue(item.getTotalRevenue())
                        .hotdealQuantity(item.getHotdealQuantity())
                        .hotdealRevenue(item.getHotdealRevenue())
                        .regularQuantity(item.getRegularQuantity())
                        .regularRevenue(item.getRegularRevenue())
                        .wasPartOfHotdeal(item.getWasPartOfHotdeal())
                        .build())
                .collect(Collectors.toList());

        return DailyStatisticResponseDto.builder()
                .date(date)
                .overallSummary(overallSummary)
                .hotdealAnalysis(hotdealAnalysis)
                .hourlyBreakdown(hourlyBreakdown)
                .soldItemsSummary(soldItemsSummary)
                .changeRate(changeRate)
                .build();
    }

    private WeeklyStatisticResponseDto buildWeeklyStatisticResponseDto(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            OrderRepository.ComprehensiveStatsProjection stats,
            List<OrderRepository.DailyBreakdownWithHotdealProjection> dailyData,
            List<OrderRepository.SoldItemsSummaryProjection> soldItemsData,
            double changeRate) {

        // Build overall summary
        WeeklyStatisticResponseDto.OverallSummary overallSummary = WeeklyStatisticResponseDto.OverallSummary.builder()
                .totalSalesAmount(stats.getTotalSalesAmount())
                .totalOrderCount(stats.getTotalOrderCount())
                .totalItemCount(stats.getTotalItemCount())
                .averageDailySalesAmount(stats.getTotalSalesAmount() / 7)
                .uniqueCustomerCount(stats.getUniqueCustomerCount())
                .bestPerformingDay(findBestPerformingDay(dailyData))
                .bestDaySalesAmount(findBestDaySales(dailyData))
                .build();

        // Build hotdeal analysis
        WeeklyStatisticResponseDto.HotdealAnalysis hotdealAnalysis = buildWeeklyHotdealAnalysis(stats);

        // Build daily breakdown
        List<WeeklyStatisticResponseDto.DailyBreakdown> dailyBreakdown = dailyData.stream()
                .map(d -> WeeklyStatisticResponseDto.DailyBreakdown.builder()
                        .dayOfWeek(d.getDayOfWeek().trim())
                        .date(d.getOrderDate())
                        .salesAmount(d.getSalesAmount())
                        .orderCount(d.getOrderCount())
                        .hotdealSalesAmount(d.getHotdealSalesAmount())
                        .regularSalesAmount(d.getRegularSalesAmount())
                        .build())
                .collect(Collectors.toList());

        // Build sold items summary
        List<WeeklyStatisticResponseDto.SoldItemSummary> soldItemsSummary = soldItemsData.stream()
                .map(item -> WeeklyStatisticResponseDto.SoldItemSummary.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .totalQuantity(item.getTotalQuantity())
                        .totalRevenue(item.getTotalRevenue())
                        .hotdealQuantity(item.getHotdealQuantity())
                        .hotdealRevenue(item.getHotdealRevenue())
                        .regularQuantity(item.getRegularQuantity())
                        .regularRevenue(item.getRegularRevenue())
                        .wasPartOfHotdeal(item.getWasPartOfHotdeal())
                        .build())
                .collect(Collectors.toList());

        return WeeklyStatisticResponseDto.builder()
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .overallSummary(overallSummary)
                .hotdealAnalysis(hotdealAnalysis)
                .dailyBreakdown(dailyBreakdown)
                .soldItemsSummary(soldItemsSummary)
                .changeRate(changeRate)
                .build();
    }

    private MonthlyStatisticResponseDto buildMonthlyStatisticResponseDto(
            int year,
            int month,
            LocalDate monthStart,
            LocalDate monthEnd,
            OrderRepository.ComprehensiveStatsProjection stats,
            List<OrderRepository.WeeklyBreakdownWithHotdealProjection> weeklyData,
            List<OrderRepository.SoldItemsSummaryProjection> soldItemsData,
            double changeRate) {

        // Build overall summary
        MonthlyStatisticResponseDto.OverallSummary overallSummary = MonthlyStatisticResponseDto.OverallSummary.builder()
                .totalSalesAmount(stats.getTotalSalesAmount())
                .totalOrderCount(stats.getTotalOrderCount())
                .totalItemCount(stats.getTotalItemCount())
                .averageDailySalesAmount(stats.getTotalSalesAmount() / monthStart.lengthOfMonth())
                .uniqueCustomerCount(stats.getUniqueCustomerCount())
                .bestPerformingWeek(findBestPerformingWeek(weeklyData))
                .bestWeekSalesAmount(findBestWeekSales(weeklyData))
                .build();

        // Build hotdeal analysis
        MonthlyStatisticResponseDto.HotdealAnalysis hotdealAnalysis = buildMonthlyHotdealAnalysis(stats);

        // Build weekly breakdown
        List<MonthlyStatisticResponseDto.WeeklyBreakdown> weeklyBreakdown = weeklyData.stream()
                .map(w -> MonthlyStatisticResponseDto.WeeklyBreakdown.builder()
                        .weekNumber(w.getWeekNumber())
                        .weekStartDate(w.getWeekStartDate())
                        .weekEndDate(w.getWeekEndDate())
                        .salesAmount(w.getSalesAmount())
                        .orderCount(w.getOrderCount())
                        .hotdealSalesAmount(w.getHotdealSalesAmount())
                        .regularSalesAmount(w.getRegularSalesAmount())
                        .build())
                .collect(Collectors.toList());

        // Build sold items summary
        List<MonthlyStatisticResponseDto.SoldItemSummary> soldItemsSummary = soldItemsData.stream()
                .map(item -> MonthlyStatisticResponseDto.SoldItemSummary.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .totalQuantity(item.getTotalQuantity())
                        .totalRevenue(item.getTotalRevenue())
                        .hotdealQuantity(item.getHotdealQuantity())
                        .hotdealRevenue(item.getHotdealRevenue())
                        .regularQuantity(item.getRegularQuantity())
                        .regularRevenue(item.getRegularRevenue())
                        .wasPartOfHotdeal(item.getWasPartOfHotdeal())
                        .build())
                .collect(Collectors.toList());

        return MonthlyStatisticResponseDto.builder()
                .month(String.format("%04d-%02d", year, month))
                .monthStartDate(monthStart)
                .monthEndDate(monthEnd)
                .overallSummary(overallSummary)
                .hotdealAnalysis(hotdealAnalysis)
                .weeklyBreakdown(weeklyBreakdown)
                .soldItemsSummary(soldItemsSummary)
                .changeRate(changeRate)
                .build();
    }

    // ========================================
    // Utility Helper Methods
    // ========================================

    private DailyStatisticResponseDto.HotdealAnalysis buildHotdealAnalysis(OrderRepository.ComprehensiveStatsProjection stats) {
        // Calculate hotdeal conversion rate
        double hotdealConversionRate = stats.getTotalOrderCount() > 0
            ? (double) stats.getHotdealOrderCount() / stats.getTotalOrderCount() * 100
            : 0.0;

        DailyStatisticResponseDto.HotdealStats hotdealStats = DailyStatisticResponseDto.HotdealStats.builder()
                .salesAmount(stats.getHotdealSalesAmount())
                .orderCount(stats.getHotdealOrderCount())
                .itemCount(stats.getHotdealItemCount())
                .averageOrderAmount(stats.getHotdealOrderCount() > 0 ? stats.getHotdealSalesAmount() / stats.getHotdealOrderCount() : 0L)
                .build();

        DailyStatisticResponseDto.RegularStats regularStats = DailyStatisticResponseDto.RegularStats.builder()
                .salesAmount(stats.getRegularSalesAmount())
                .orderCount(stats.getRegularOrderCount())
                .itemCount(stats.getRegularItemCount())
                .averageOrderAmount(stats.getRegularOrderCount() > 0 ? stats.getRegularSalesAmount() / stats.getRegularOrderCount() : 0L)
                .build();

        return DailyStatisticResponseDto.HotdealAnalysis.builder()
                .hotdealSales(hotdealStats)
                .regularSales(regularStats)
                .hotdealConversionRate(hotdealConversionRate)
                .build();
    }

    private WeeklyStatisticResponseDto.HotdealAnalysis buildWeeklyHotdealAnalysis(OrderRepository.ComprehensiveStatsProjection stats) {
        double hotdealConversionRate = stats.getTotalOrderCount() > 0
            ? (double) stats.getHotdealOrderCount() / stats.getTotalOrderCount() * 100
            : 0.0;

        WeeklyStatisticResponseDto.HotdealStats hotdealStats = WeeklyStatisticResponseDto.HotdealStats.builder()
                .salesAmount(stats.getHotdealSalesAmount())
                .orderCount(stats.getHotdealOrderCount())
                .itemCount(stats.getHotdealItemCount())
                .averageOrderAmount(stats.getHotdealOrderCount() > 0 ? stats.getHotdealSalesAmount() / stats.getHotdealOrderCount() : 0L)
                .build();

        WeeklyStatisticResponseDto.RegularStats regularStats = WeeklyStatisticResponseDto.RegularStats.builder()
                .salesAmount(stats.getRegularSalesAmount())
                .orderCount(stats.getRegularOrderCount())
                .itemCount(stats.getRegularItemCount())
                .averageOrderAmount(stats.getRegularOrderCount() > 0 ? stats.getRegularSalesAmount() / stats.getRegularOrderCount() : 0L)
                .build();

        return WeeklyStatisticResponseDto.HotdealAnalysis.builder()
                .hotdealSales(hotdealStats)
                .regularSales(regularStats)
                .hotdealConversionRate(hotdealConversionRate)
                .build();
    }

    private MonthlyStatisticResponseDto.HotdealAnalysis buildMonthlyHotdealAnalysis(OrderRepository.ComprehensiveStatsProjection stats) {
        double hotdealConversionRate = stats.getTotalOrderCount() > 0
            ? (double) stats.getHotdealOrderCount() / stats.getTotalOrderCount() * 100
            : 0.0;

        MonthlyStatisticResponseDto.HotdealStats hotdealStats = MonthlyStatisticResponseDto.HotdealStats.builder()
                .salesAmount(stats.getHotdealSalesAmount())
                .orderCount(stats.getHotdealOrderCount())
                .itemCount(stats.getHotdealItemCount())
                .averageOrderAmount(stats.getHotdealOrderCount() > 0 ? stats.getHotdealSalesAmount() / stats.getHotdealOrderCount() : 0L)
                .build();

        MonthlyStatisticResponseDto.RegularStats regularStats = MonthlyStatisticResponseDto.RegularStats.builder()
                .salesAmount(stats.getRegularSalesAmount())
                .orderCount(stats.getRegularOrderCount())
                .itemCount(stats.getRegularItemCount())
                .averageOrderAmount(stats.getRegularOrderCount() > 0 ? stats.getRegularSalesAmount() / stats.getRegularOrderCount() : 0L)
                .build();

        return MonthlyStatisticResponseDto.HotdealAnalysis.builder()
                .hotdealSales(hotdealStats)
                .regularSales(regularStats)
                .hotdealConversionRate(hotdealConversionRate)
                .build();
    }

    private Integer findPeakHour(List<OrderRepository.HourlyBreakdownProjection> hourlyData) {
        return hourlyData.stream()
                .max((h1, h2) -> Long.compare(h1.getSalesAmount(), h2.getSalesAmount()))
                .map(OrderRepository.HourlyBreakdownProjection::getHour)
                .orElse(0);
    }

    private Long findPeakHourSales(List<OrderRepository.HourlyBreakdownProjection> hourlyData) {
        return hourlyData.stream()
                .mapToLong(OrderRepository.HourlyBreakdownProjection::getSalesAmount)
                .max()
                .orElse(0L);
    }

    private String findBestPerformingDay(List<OrderRepository.DailyBreakdownWithHotdealProjection> dailyData) {
        return dailyData.stream()
                .max((d1, d2) -> Long.compare(d1.getSalesAmount(), d2.getSalesAmount()))
                .map(d -> d.getDayOfWeek().trim())
                .orElse("MONDAY");
    }

    private Long findBestDaySales(List<OrderRepository.DailyBreakdownWithHotdealProjection> dailyData) {
        return dailyData.stream()
                .mapToLong(OrderRepository.DailyBreakdownWithHotdealProjection::getSalesAmount)
                .max()
                .orElse(0L);
    }

    private String findBestPerformingWeek(List<OrderRepository.WeeklyBreakdownWithHotdealProjection> weeklyData) {
        return weeklyData.stream()
                .max((w1, w2) -> Long.compare(w1.getSalesAmount(), w2.getSalesAmount()))
                .map(w -> w.getWeekStartDate() + " to " + w.getWeekEndDate())
                .orElse("Week 1");
    }

    private Long findBestWeekSales(List<OrderRepository.WeeklyBreakdownWithHotdealProjection> weeklyData) {
        return weeklyData.stream()
                .mapToLong(OrderRepository.WeeklyBreakdownWithHotdealProjection::getSalesAmount)
                .max()
                .orElse(0L);
    }

}
