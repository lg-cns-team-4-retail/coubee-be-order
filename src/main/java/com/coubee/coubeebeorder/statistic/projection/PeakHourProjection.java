package com.coubee.coubeebeorder.statistic.projection;

/**
 * JPA Projection interface for peak hour statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface PeakHourProjection {
    
    /**
     * Hour of the day (0-23)
     * @return hour as Integer
     */
    Integer getHour();
    
    /**
     * Sales amount during this hour
     * @return hourly sales amount as Long
     */
    Long getHourlySales();
}
