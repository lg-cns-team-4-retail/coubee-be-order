package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.StoreServiceException;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticResponseDto;
import com.coubee.coubeebeorder.statistic.dto.ProductSalesSummaryDto;
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
    @CircuitBreaker(name = "downstreamServices", fallbackMethod = "validateStoreAccessFallback")
    private void validateStoreAccess(Long userId, Long storeId) {
        if (storeId == null) {
            return; // 시스템 전체 통계는 매장 검증이 필요하지 않음
        }

        try {
            // 스토어 서비스에서 사용자가 소유한 승인된 매장 목록 조회
            ApiResponseDto<List<Long>> ownedStoresResponse = storeClient.getStoresByOwnerIdOnApproved(userId);
            List<Long> ownedStoreIds = ownedStoresResponse.getData();

            if (ownedStoreIds == null || !ownedStoreIds.contains(storeId)) {
                throw new IllegalArgumentException("You can only view statistics for registered stores you own.");
            }

            // 프로덕션 호환성을 위해 매장 상태 검증 임시 비활성화
            // isStoreApproved API가 아직 프로덕션 메인 브랜치에서 사용할 수 없음
            /*
            ApiResponseDto<Boolean> storeStatusResponse = storeClient.isStoreApproved(storeId);
            Boolean isApproved = storeStatusResponse.getData();

            if (isApproved == null || !isApproved) {
                throw new ApiError("Access denied: Store is not approved");
            }
            */
        } catch (IllegalArgumentException e) {
            // 권한 예외는 그대로 다시 던져서 403을 유도합니다.
            throw e;
        } catch (Exception e) {
            // FeignException 등 통신 오류는 여기서 잡힙니다.
            log.error("Error validating store access for userId: {}, storeId: {}", userId, storeId, e);
            // 더 명확한 서비스 장애 예외를 던집니다.
            throw new StoreServiceException("Unable to validate store access due to a dependent service issue.");
        }
    }

    /**
     * validateStoreAccess의 폴백 메소드
     */
    private void validateStoreAccessFallback(Long userId, Long storeId, Exception ex) {
        log.warn("Circuit breaker for validateStoreAccess is open. userId: {}, storeId: {}. Error: {}", userId, storeId, ex.getMessage());
        // Store 서비스 장애 시, 명확한 예외를 던져 사용자에게 알립니다.
        throw new StoreServiceException("매장 정보를 조회하는 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * Calculate percentage change between current and previous values
     *
     * @param currentValue the current period value
     * @param previousValue the previous period value
     * @return percentage change as a double
     */
    private double calculatePercentageChange(Long currentValue, Long previousValue) {
        // null 값을 0으로 처리
        long current = currentValue != null ? currentValue : 0L;
        long previous = previousValue != null ? previousValue : 0L;

        // 예외 상황 처리
        if (previous == 0) {
            // 이전 기간 매출이 0인 경우
            if (current > 0) {
                return 100.0; // 100% 증가
            } else {
                return 0.0; // 변화 없음 (둘 다 0)
            }
        }

        if (current == 0 && previous > 0) {
            return -100.0; // 100% 감소
        }

        // 백분율 변화 계산: ((현재 - 이전) / 이전) * 100
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
                return null; // 피크 시간 데이터 없음
            }

            @Override
            public Long getHourlySales() {
                return 0L; // 매출 데이터 없음
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
                return "데이터 없음"; // 최고 매출일 데이터 없음
            }

            @Override
            public Long getDailySales() {
                return 0L; // 매출 데이터 없음
            }
        };
    }

    @Override
    public List<ProductSalesSummaryDto> getProductSalesSummary(Long storeId, LocalDate startDate, LocalDate endDate, Long userId) {
        log.info("Getting product sales summary with hotdeal data for storeId: {}, startDate: {}, endDate: {}, userId: {}",
                storeId, startDate, endDate, userId);

        // Validate store access
        validateStoreAccess(userId, storeId);

        try {
            // LocalDate를 UNIX 타임스탬프로 변환
            long startUnix = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long endUnix = endDate.atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);

            // 핫딜 데이터가 포함된 새로운 쿼리 메소드를 호출합니다.
            List<OrderRepository.SoldItemsSummaryProjection> projections =
                orderRepository.getSoldItemsSummaryWithHotdeal(startUnix, endUnix, storeId);

            // 수정된 DTO에 맞게 프로젝션 결과를 매핑합니다. (translation: Map the projection results to the modified DTO.)
            List<ProductSalesSummaryDto> result = projections.stream()
                .map(projection -> ProductSalesSummaryDto.builder()
                    .productId(projection.getProductId())
                    .productName(projection.getProductName())
                    .totalQuantitySold(projection.getTotalQuantity())
                    .totalSalesAmount(projection.getTotalRevenue())
                    .hotdealQuantitySold(projection.getHotdealQuantity())
                    .hotdealSalesAmount(projection.getHotdealRevenue())
                    .regularQuantitySold(projection.getRegularQuantity())
                    .regularSalesAmount(projection.getRegularRevenue())
                    .build())
                .collect(Collectors.toList());

            log.info("Successfully retrieved product sales summary with hotdeal data for storeId: {}, found {} products",
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
