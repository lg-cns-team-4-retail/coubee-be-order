package com.coubee.repository;

import com.coubee.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access interface for order item entity
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find order items by order ID
     *
     * @param orderId Order ID
     * @return List of order items
     */
    List<OrderItem> findByOrderOrderId(String orderId);
} 
