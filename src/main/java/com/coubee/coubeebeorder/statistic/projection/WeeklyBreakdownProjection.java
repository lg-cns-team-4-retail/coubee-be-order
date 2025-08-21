package com.coubee.coubeebeorder.statistic.projection;

import java.time.LocalDate;

/**
 * JPA Projection interface for weekly breakdown statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface WeeklyBreakdownProjection {
    
    /**
     * Week number within the month
     * @return week number as Integer
     */
    Integer getWeekNumber();
    
    /**
     * Week start date
     * @return week start date as LocalDate
     */
    LocalDate getWeekStartDate();
    
    /**
     * Week end date
     * @return week end date as LocalDate
     */
    LocalDate getWeekEndDate();
    
    /**
     * Sales amount for this week
     * @return sales amount as Long
     */
    Long getSalesAmount();
    
    /**
     * Number of orders for this week
     * @return order count as Integer
     */
    Integer getOrderCount();
}
