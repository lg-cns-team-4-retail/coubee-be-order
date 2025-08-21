package com.coubee.coubeebeorder.statistic.projection;

/**
 * JPA Projection interface for total item count statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface TotalItemCountProjection {
    
    /**
     * Total item count from order items
     * @return total item count as Integer
     */
    Integer getTotalItemCount();
}
