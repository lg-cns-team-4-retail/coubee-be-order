package com.coubee.coubeebeorder.statistic.projection;

/**
 * JPA Projection interface for best performing day statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface BestDayProjection {
    
    /**
     * Name of the day (e.g., "MONDAY", "TUESDAY")
     * @return day name as String
     */
    String getDayName();
    
    /**
     * Sales amount for this day
     * @return daily sales amount as Long
     */
    Long getDailySales();
}
