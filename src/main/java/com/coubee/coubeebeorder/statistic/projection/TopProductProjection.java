package com.coubee.coubeebeorder.statistic.projection;

/**
 * JPA Projection interface for top product statistics
 * Replaces manual object mapping from JdbcTemplate ResultSet
 */
public interface TopProductProjection {
    
    /**
     * Product ID
     * @return product ID as Long
     */
    Long getProductId();
    
    /**
     * Product name
     * @return product name as String
     */
    String getProductName();
    
    /**
     * Total quantity sold
     * @return quantity sold as Integer
     */
    Integer getQuantitySold();
    
    /**
     * Total sales amount for this product
     * @return sales amount as Long
     */
    Long getSalesAmount();
}
