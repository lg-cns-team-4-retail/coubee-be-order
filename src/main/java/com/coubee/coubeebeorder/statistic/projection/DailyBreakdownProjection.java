package com.coubee.coubeebeorder.statistic.projection;

import java.time.LocalDate;

/**
 * JPA Projection interface for daily breakdown statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface DailyBreakdownProjection {
    
    /**
     * Day of the week (e.g., "MONDAY", "TUESDAY")
     * @return day of week as String
     */
    String getDayOfWeek();
    
    /**
     * Order date
     * @return order date as LocalDate
     */
    LocalDate getOrderDate();
    
    /**
     * Sales amount for this day
     * @return sales amount as Long
     */
    Long getSalesAmount();
    
    /**
     * Number of orders for this day
     * @return order count as Integer
     */
    Integer getOrderCount();
}
