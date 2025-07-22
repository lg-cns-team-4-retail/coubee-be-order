package com.coubee.repository;

import com.coubee.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Order entity data access operations
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by order ID
     *
     * @param orderId Order ID
     * @return Order Optional
     */
    Optional<Order> findByOrderId(String orderId);

    /**
     * Find orders by user ID with pagination
     *
     * @param userId User ID
     * @param pageable Pagination information
     * @return Order page
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
} 
