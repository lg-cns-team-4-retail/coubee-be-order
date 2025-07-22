package com.coubee.kafka.producer.stock.event;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Stock Decrease Event
 */
@Getter
@Builder
public class StockDecreaseEvent {

    /**
     * Order ID
     */
    private String orderId;

    /**
     * List of items for stock decrease
     */
    private List<StockItem> items;

    /**
     * Stock item information
     */
    @Getter
    @Builder
    public static class StockItem {

        /**
         * Product ID
         */
        private Long productId;

        /**
         * Quantity
         */
        private Integer quantity;
    }
} 
