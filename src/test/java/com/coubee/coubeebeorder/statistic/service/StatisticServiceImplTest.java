package com.coubee.coubeebeorder.statistic.service;

import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.statistic.dto.DailyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.MonthlyStatisticDto;
import com.coubee.coubeebeorder.statistic.dto.WeeklyStatisticDto;
import com.coubee.coubeebeorder.statistic.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("통계 서비스 구현체 테스트 - JPA 리팩토링")
class StatisticServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StatisticServiceImpl statisticService;

    private LocalDate testDate;
    private LocalDate testWeekStartDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2023, 6, 15);
        testWeekStartDate = LocalDate.of(2023, 6, 12); // Monday
    }

    @Test
    @DisplayName("일일 통계 조회 - 변화율 계산 (증가)")
    void dailyStatistic_ChangeRate_Increase() {
        // Given
        OrderAggregationProjection todayOrderStats = createOrderAggregationProjection(150000L, 10, 5);
        OrderAggregationProjection yesterdayOrderStats = createOrderAggregationProjection(100000L, 8, 4);
        TotalItemCountProjection todayItemCount = createTotalItemCountProjection(25);
        PeakHourProjection peakHour = createPeakHourProjection(14, 50000L);

        given(orderRepository.getOrderAggregation(anyLong(), anyLong(), any())).willReturn(todayOrderStats, yesterdayOrderStats);
        given(orderRepository.getTotalItemCount(anyLong(), anyLong(), any())).willReturn(todayItemCount);
        given(orderRepository.getPeakHour(anyLong(), anyLong(), any())).willReturn(peakHour);

        // When
        DailyStatisticDto result = statisticService.dailyStatistic(testDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(150000L);
        assertThat(result.getTotalOrderCount()).isEqualTo(10);
        assertThat(result.getTotalItemCount()).isEqualTo(25);
        assertThat(result.getUniqueCustomerCount()).isEqualTo(5);
        assertThat(result.getPeakHour()).isEqualTo(14);
        assertThat(result.getPeakHourSalesAmount()).isEqualTo(50000L);
        assertThat(result.getChangeRate()).isEqualTo(50.0); // (150000 - 100000) / 100000 * 100 = 50%
    }

    @Test
    @DisplayName("일일 통계 조회 - 변화율 계산 (감소)")
    void dailyStatistic_ChangeRate_Decrease() {
        // Given
        DailyStatisticDto todayStats = createDailyStatisticDto(testDate, 80000L);
        DailyStatisticDto yesterdayStats = createDailyStatisticDto(testDate.minusDays(1), 100000L);

        given(statisticRepository.getDailyStatistic(testDate, null)).willReturn(todayStats);
        given(statisticRepository.getDailyStatistic(testDate.minusDays(1), null)).willReturn(yesterdayStats);

        // When
        DailyStatisticDto result = statisticService.dailyStatistic(testDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(80000L);
        assertThat(result.getChangeRate()).isEqualTo(-20.0); // (80000 - 100000) / 100000 * 100 = -20%
    }

    @Test
    @DisplayName("일일 통계 조회 - 이전 매출이 0인 경우")
    void dailyStatistic_ChangeRate_PreviousZero() {
        // Given
        DailyStatisticDto todayStats = createDailyStatisticDto(testDate, 100000L);
        DailyStatisticDto yesterdayStats = createDailyStatisticDto(testDate.minusDays(1), 0L);

        given(statisticRepository.getDailyStatistic(testDate, null)).willReturn(todayStats);
        given(statisticRepository.getDailyStatistic(testDate.minusDays(1), null)).willReturn(yesterdayStats);

        // When
        DailyStatisticDto result = statisticService.dailyStatistic(testDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(100000L);
        assertThat(result.getChangeRate()).isEqualTo(100.0); // 100% increase from zero
    }

    @Test
    @DisplayName("일일 통계 조회 - 현재 매출이 0인 경우")
    void dailyStatistic_ChangeRate_CurrentZero() {
        // Given
        DailyStatisticDto todayStats = createDailyStatisticDto(testDate, 0L);
        DailyStatisticDto yesterdayStats = createDailyStatisticDto(testDate.minusDays(1), 100000L);

        given(statisticRepository.getDailyStatistic(testDate, null)).willReturn(todayStats);
        given(statisticRepository.getDailyStatistic(testDate.minusDays(1), null)).willReturn(yesterdayStats);

        // When
        DailyStatisticDto result = statisticService.dailyStatistic(testDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(0L);
        assertThat(result.getChangeRate()).isEqualTo(-100.0); // 100% decrease
    }

    @Test
    @DisplayName("일일 통계 조회 - 둘 다 0인 경우")
    void dailyStatistic_ChangeRate_BothZero() {
        // Given
        DailyStatisticDto todayStats = createDailyStatisticDto(testDate, 0L);
        DailyStatisticDto yesterdayStats = createDailyStatisticDto(testDate.minusDays(1), 0L);

        given(statisticRepository.getDailyStatistic(testDate, null)).willReturn(todayStats);
        given(statisticRepository.getDailyStatistic(testDate.minusDays(1), null)).willReturn(yesterdayStats);

        // When
        DailyStatisticDto result = statisticService.dailyStatistic(testDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(0L);
        assertThat(result.getChangeRate()).isEqualTo(0.0); // No change
    }

    @Test
    @DisplayName("주간 통계 조회 - 변화율 계산")
    void weeklyStatistic_ChangeRate_Calculation() {
        // Given
        WeeklyStatisticDto currentWeekStats = createWeeklyStatisticDto(testWeekStartDate, 1050000L);
        WeeklyStatisticDto previousWeekStats = createWeeklyStatisticDto(testWeekStartDate.minusWeeks(1), 900000L);

        given(statisticRepository.getWeeklyStatistic(testWeekStartDate, null)).willReturn(currentWeekStats);
        given(statisticRepository.getWeeklyStatistic(testWeekStartDate.minusWeeks(1), null)).willReturn(previousWeekStats);

        // When
        WeeklyStatisticDto result = statisticService.weeklyStatistic(testWeekStartDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(1050000L);
        // (1050000 - 900000) / 900000 * 100 = 16.67%
        assertThat(result.getChangeRate()).isCloseTo(16.67, org.assertj.core.data.Offset.offset(0.01));

        verify(statisticRepository).getWeeklyStatistic(testWeekStartDate, null);
        verify(statisticRepository).getWeeklyStatistic(testWeekStartDate.minusWeeks(1), null);
    }

    @Test
    @DisplayName("월간 통계 조회 - 변화율 계산")
    void monthlyStatistic_ChangeRate_Calculation() {
        // Given
        int year = 2023;
        int month = 6;
        MonthlyStatisticDto currentMonthStats = createMonthlyStatisticDto(year, month, 4500000L);
        MonthlyStatisticDto previousMonthStats = createMonthlyStatisticDto(2023, 5, 4000000L);

        given(statisticRepository.getMonthlyStatistic(year, month, null)).willReturn(currentMonthStats);
        given(statisticRepository.getMonthlyStatistic(2023, 5, null)).willReturn(previousMonthStats);

        // When
        MonthlyStatisticDto result = statisticService.monthlyStatistic(year, month, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(4500000L);
        assertThat(result.getChangeRate()).isEqualTo(12.5); // (4500000 - 4000000) / 4000000 * 100 = 12.5%

        verify(statisticRepository).getMonthlyStatistic(year, month, null);
        verify(statisticRepository).getMonthlyStatistic(2023, 5, null);
    }

    @Test
    @DisplayName("월간 통계 조회 - 연도 경계 처리")
    void monthlyStatistic_ChangeRate_YearBoundary() {
        // Given - January 2024, previous month is December 2023
        int year = 2024;
        int month = 1;
        MonthlyStatisticDto currentMonthStats = createMonthlyStatisticDto(year, month, 3000000L);
        MonthlyStatisticDto previousMonthStats = createMonthlyStatisticDto(2023, 12, 2500000L);

        given(statisticRepository.getMonthlyStatistic(year, month, null)).willReturn(currentMonthStats);
        given(statisticRepository.getMonthlyStatistic(2023, 12, null)).willReturn(previousMonthStats);

        // When
        MonthlyStatisticDto result = statisticService.monthlyStatistic(year, month, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(3000000L);
        assertThat(result.getChangeRate()).isEqualTo(20.0); // (3000000 - 2500000) / 2500000 * 100 = 20%

        verify(statisticRepository).getMonthlyStatistic(2024, 1, null);
        verify(statisticRepository).getMonthlyStatistic(2023, 12, null);
    }

    @Test
    @DisplayName("일일 통계 조회 - 특정 매장 필터링")
    void dailyStatistic_WithStoreId() {
        // Given
        Long storeId = 1L;
        DailyStatisticDto todayStats = createDailyStatisticDto(testDate, 50000L);
        DailyStatisticDto yesterdayStats = createDailyStatisticDto(testDate.minusDays(1), 40000L);

        given(statisticRepository.getDailyStatistic(testDate, storeId)).willReturn(todayStats);
        given(statisticRepository.getDailyStatistic(testDate.minusDays(1), storeId)).willReturn(yesterdayStats);

        // When
        DailyStatisticDto result = statisticService.dailyStatistic(testDate, storeId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(50000L);
        assertThat(result.getChangeRate()).isEqualTo(25.0); // (50000 - 40000) / 40000 * 100 = 25%

        verify(statisticRepository).getDailyStatistic(testDate, storeId);
        verify(statisticRepository).getDailyStatistic(testDate.minusDays(1), storeId);
    }

    @Test
    @DisplayName("월간 통계 조회 - 고급 통계 기능 검증")
    void monthlyStatistic_AdvancedFeatures() {
        // Given
        int year = 2023;
        int month = 6;

        // Create mock monthly stats with advanced features
        MonthlyStatisticDto currentMonthStats = createAdvancedMonthlyStatisticDto(year, month, 4500000L);
        MonthlyStatisticDto previousMonthStats = createAdvancedMonthlyStatisticDto(2023, 5, 4000000L);

        given(statisticRepository.getMonthlyStatistic(year, month, null)).willReturn(currentMonthStats);
        given(statisticRepository.getMonthlyStatistic(2023, 5, null)).willReturn(previousMonthStats);

        // When
        MonthlyStatisticDto result = statisticService.monthlyStatistic(year, month, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(4500000L);
        assertThat(result.getChangeRate()).isEqualTo(12.5); // (4500000 - 4000000) / 4000000 * 100 = 12.5%

        // Verify advanced features are populated
        assertThat(result.getTopProducts()).isNotEmpty();
        assertThat(result.getTopProducts()).hasSize(2); // Mock data has 2 products
        assertThat(result.getTopProducts().get(0).getProductName()).isEqualTo("테스트 상품 1");

        assertThat(result.getWeeklyBreakdown()).isNotEmpty();
        assertThat(result.getWeeklyBreakdown()).hasSize(2); // Mock data has 2 weeks
        assertThat(result.getWeeklyBreakdown().get(0).getWeekNumber()).isEqualTo(1);

        verify(statisticRepository).getMonthlyStatistic(year, month, null);
        verify(statisticRepository).getMonthlyStatistic(2023, 5, null);
    }

    @Test
    @DisplayName("주간 통계 조회 - 고급 통계 기능 검증")
    void weeklyStatistic_AdvancedFeatures() {
        // Given
        WeeklyStatisticDto currentWeekStats = createAdvancedWeeklyStatisticDto(testWeekStartDate, 1050000L);
        WeeklyStatisticDto previousWeekStats = createAdvancedWeeklyStatisticDto(testWeekStartDate.minusWeeks(1), 900000L);

        given(statisticRepository.getWeeklyStatistic(testWeekStartDate, null)).willReturn(currentWeekStats);
        given(statisticRepository.getWeeklyStatistic(testWeekStartDate.minusWeeks(1), null)).willReturn(previousWeekStats);

        // When
        WeeklyStatisticDto result = statisticService.weeklyStatistic(testWeekStartDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalSalesAmount()).isEqualTo(1050000L);
        // (1050000 - 900000) / 900000 * 100 = 16.67%
        assertThat(result.getChangeRate()).isCloseTo(16.67, org.assertj.core.data.Offset.offset(0.01));

        // Verify advanced features are populated
        assertThat(result.getBestPerformingDay()).isEqualTo("FRIDAY");
        assertThat(result.getBestDaySalesAmount()).isEqualTo(200000L);

        assertThat(result.getDailyBreakdown()).isNotEmpty();
        assertThat(result.getDailyBreakdown()).hasSize(3); // Mock data has 3 days
        assertThat(result.getDailyBreakdown().get(0).getDayOfWeek()).isEqualTo("MONDAY");

        verify(statisticRepository).getWeeklyStatistic(testWeekStartDate, null);
        verify(statisticRepository).getWeeklyStatistic(testWeekStartDate.minusWeeks(1), null);
    }

    // Helper methods for creating test DTOs
    private DailyStatisticDto createDailyStatisticDto(LocalDate date, Long totalSalesAmount) {
        return DailyStatisticDto.builder()
                .date(date)
                .totalSalesAmount(totalSalesAmount)
                .totalOrderCount(25)
                .totalItemCount(45)
                .averageOrderAmount(totalSalesAmount != null && totalSalesAmount > 0 ? totalSalesAmount / 25 : 0L)
                .uniqueCustomerCount(20)
                .peakHour(14)
                .peakHourSalesAmount(25000L)
                .build();
    }

    private WeeklyStatisticDto createWeeklyStatisticDto(LocalDate weekStartDate, Long totalSalesAmount) {
        return WeeklyStatisticDto.builder()
                .weekStartDate(weekStartDate)
                .weekEndDate(weekStartDate.plusDays(6))
                .totalSalesAmount(totalSalesAmount)
                .totalOrderCount(175)
                .totalItemCount(315)
                .averageDailySalesAmount(totalSalesAmount != null ? totalSalesAmount / 7 : 0L)
                .uniqueCustomerCount(140)
                .bestPerformingDay("FRIDAY")
                .bestDaySalesAmount(200000L)
                .dailyBreakdown(new ArrayList<>())
                .build();
    }

    private MonthlyStatisticDto createMonthlyStatisticDto(int year, int month, Long totalSalesAmount) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        
        return MonthlyStatisticDto.builder()
                .month(String.format("%04d-%02d", year, month))
                .monthStartDate(monthStart)
                .monthEndDate(monthEnd)
                .totalSalesAmount(totalSalesAmount)
                .totalOrderCount(750)
                .totalItemCount(1350)
                .averageDailySalesAmount(totalSalesAmount != null ? totalSalesAmount / monthStart.lengthOfMonth() : 0L)
                .uniqueCustomerCount(600)
                .bestPerformingWeek("Week 1")
                .bestWeekSalesAmount(1200000L)
                .topProducts(new ArrayList<>())
                .weeklyBreakdown(new ArrayList<>())
                .build();
    }

    private WeeklyStatisticDto createAdvancedWeeklyStatisticDto(LocalDate weekStartDate, Long totalSalesAmount) {
        // Create mock daily breakdown
        List<WeeklyStatisticDto.DailyBreakdown> dailyBreakdown = List.of(
                WeeklyStatisticDto.DailyBreakdown.builder()
                        .dayOfWeek("MONDAY")
                        .date(weekStartDate)
                        .salesAmount(120000L)
                        .orderCount(20)
                        .build(),
                WeeklyStatisticDto.DailyBreakdown.builder()
                        .dayOfWeek("WEDNESDAY")
                        .date(weekStartDate.plusDays(2))
                        .salesAmount(150000L)
                        .orderCount(25)
                        .build(),
                WeeklyStatisticDto.DailyBreakdown.builder()
                        .dayOfWeek("FRIDAY")
                        .date(weekStartDate.plusDays(4))
                        .salesAmount(200000L)
                        .orderCount(35)
                        .build()
        );

        return WeeklyStatisticDto.builder()
                .weekStartDate(weekStartDate)
                .weekEndDate(weekStartDate.plusDays(6))
                .totalSalesAmount(totalSalesAmount)
                .totalOrderCount(175)
                .totalItemCount(315)
                .averageDailySalesAmount(totalSalesAmount != null ? totalSalesAmount / 7 : 0L)
                .uniqueCustomerCount(140)
                .bestPerformingDay("FRIDAY")
                .bestDaySalesAmount(200000L)
                .dailyBreakdown(dailyBreakdown)
                .build();
    }

    private MonthlyStatisticDto createAdvancedMonthlyStatisticDto(int year, int month, Long totalSalesAmount) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // Create mock top products
        List<MonthlyStatisticDto.TopProduct> topProducts = List.of(
                MonthlyStatisticDto.TopProduct.builder()
                        .productId(1L)
                        .productName("테스트 상품 1")
                        .quantitySold(150)
                        .salesAmount(300000L)
                        .build(),
                MonthlyStatisticDto.TopProduct.builder()
                        .productId(2L)
                        .productName("테스트 상품 2")
                        .quantitySold(100)
                        .salesAmount(200000L)
                        .build()
        );

        // Create mock weekly breakdown
        List<MonthlyStatisticDto.WeeklyBreakdown> weeklyBreakdown = List.of(
                MonthlyStatisticDto.WeeklyBreakdown.builder()
                        .weekNumber(1)
                        .weekStartDate(monthStart)
                        .weekEndDate(monthStart.plusDays(6))
                        .salesAmount(1200000L)
                        .orderCount(200)
                        .build(),
                MonthlyStatisticDto.WeeklyBreakdown.builder()
                        .weekNumber(2)
                        .weekStartDate(monthStart.plusDays(7))
                        .weekEndDate(monthStart.plusDays(13))
                        .salesAmount(1100000L)
                        .orderCount(180)
                        .build()
        );

        return MonthlyStatisticDto.builder()
                .month(String.format("%04d-%02d", year, month))
                .monthStartDate(monthStart)
                .monthEndDate(monthEnd)
                .totalSalesAmount(totalSalesAmount)
                .totalOrderCount(750)
                .totalItemCount(1350)
                .averageDailySalesAmount(totalSalesAmount != null ? totalSalesAmount / monthStart.lengthOfMonth() : 0L)
                .uniqueCustomerCount(600)
                .bestPerformingWeek("Week 1")
                .bestWeekSalesAmount(1200000L)
                .topProducts(topProducts)
                .weeklyBreakdown(weeklyBreakdown)
                .build();
    }

    // ========================================
    // Helper Methods for JPA Projections
    // ========================================

    private OrderAggregationProjection createOrderAggregationProjection(Long totalSalesAmount, Integer totalOrderCount, Integer uniqueCustomerCount) {
        return new OrderAggregationProjection() {
            @Override
            public Long getTotalSalesAmount() {
                return totalSalesAmount;
            }

            @Override
            public Integer getTotalOrderCount() {
                return totalOrderCount;
            }

            @Override
            public Integer getUniqueCustomerCount() {
                return uniqueCustomerCount;
            }
        };
    }

    private TotalItemCountProjection createTotalItemCountProjection(Integer totalItemCount) {
        return new TotalItemCountProjection() {
            @Override
            public Integer getTotalItemCount() {
                return totalItemCount;
            }
        };
    }

    private PeakHourProjection createPeakHourProjection(Integer hour, Long hourlySales) {
        return new PeakHourProjection() {
            @Override
            public Integer getHour() {
                return hour;
            }

            @Override
            public Long getHourlySales() {
                return hourlySales;
            }
        };
    }

    private BestDayProjection createBestDayProjection(String dayName, Long dailySales) {
        return new BestDayProjection() {
            @Override
            public String getDayName() {
                return dayName;
            }

            @Override
            public Long getDailySales() {
                return dailySales;
            }
        };
    }

    private DailyBreakdownProjection createDailyBreakdownProjection(String dayOfWeek, LocalDate orderDate, Long salesAmount, Integer orderCount) {
        return new DailyBreakdownProjection() {
            @Override
            public String getDayOfWeek() {
                return dayOfWeek;
            }

            @Override
            public LocalDate getOrderDate() {
                return orderDate;
            }

            @Override
            public Long getSalesAmount() {
                return salesAmount;
            }

            @Override
            public Integer getOrderCount() {
                return orderCount;
            }
        };
    }

    private TopProductProjection createTopProductProjection(Long productId, String productName, Integer quantitySold, Long salesAmount) {
        return new TopProductProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public String getProductName() {
                return productName;
            }

            @Override
            public Integer getQuantitySold() {
                return quantitySold;
            }

            @Override
            public Long getSalesAmount() {
                return salesAmount;
            }
        };
    }

    private WeeklyBreakdownProjection createWeeklyBreakdownProjection(Integer weekNumber, LocalDate weekStartDate, LocalDate weekEndDate, Long salesAmount, Integer orderCount) {
        return new WeeklyBreakdownProjection() {
            @Override
            public Integer getWeekNumber() {
                return weekNumber;
            }

            @Override
            public LocalDate getWeekStartDate() {
                return weekStartDate;
            }

            @Override
            public LocalDate getWeekEndDate() {
                return weekEndDate;
            }

            @Override
            public Long getSalesAmount() {
                return salesAmount;
            }

            @Override
            public Integer getOrderCount() {
                return orderCount;
            }
        };
    }
}
