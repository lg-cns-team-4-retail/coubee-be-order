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
    public DailyStatisticDto dailyStatistic(LocalDate date) {
        log.info("Getting daily statistics for date: {}", date);
        
        try {
            DailyStatisticDto result = statisticRepository.getDailyStatistic(date);
            log.info("Successfully retrieved daily statistics for date: {}, total sales: {}", 
                    date, result.getTotalSalesAmount());
            return result;
        } catch (Exception e) {
            log.error("Error retrieving daily statistics for date: {}", date, e);
            throw new RuntimeException("Failed to retrieve daily statistics", e);
        }
    }

    @Override
    public WeeklyStatisticDto weeklyStatistic(LocalDate weekStartDate) {
        log.info("Getting weekly statistics for week starting: {}", weekStartDate);
        
        try {
            WeeklyStatisticDto result = statisticRepository.getWeeklyStatistic(weekStartDate);
            log.info("Successfully retrieved weekly statistics for week starting: {}, total sales: {}", 
                    weekStartDate, result.getTotalSalesAmount());
            return result;
        } catch (Exception e) {
            log.error("Error retrieving weekly statistics for week starting: {}", weekStartDate, e);
            throw new RuntimeException("Failed to retrieve weekly statistics", e);
        }
    }

    @Override
    public MonthlyStatisticDto monthlyStatistic(int year, int month) {
        log.info("Getting monthly statistics for {}-{}", year, month);
        
        try {
            MonthlyStatisticDto result = statisticRepository.getMonthlyStatistic(year, month);
            log.info("Successfully retrieved monthly statistics for {}-{}, total sales: {}", 
                    year, month, result.getTotalSalesAmount());
            return result;
        } catch (Exception e) {
            log.error("Error retrieving monthly statistics for {}-{}", year, month, e);
            throw new RuntimeException("Failed to retrieve monthly statistics", e);
        }
    }
}
