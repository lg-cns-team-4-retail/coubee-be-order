package com.coubee.coubeebeorder.statistic.projection;

/**
 * JPA Projection interface for order aggregation statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface OrderAggregationProjection {
    
    /**
     * Total sales amount from orders
     * @return total sales amount as Long
     */
    Long getTotalSalesAmount();
    
    /**
     * Total number of orders
     * @return total order count as Integer
     */
    Integer getTotalOrderCount();
    
    /**
     * Number of unique customers
     * @return unique customer count as Integer
     */
    Integer getUniqueCustomerCount();
}
