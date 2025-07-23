package com.coubee.service;

import com.coubee.dto.request.OrderCancelRequest;
import com.coubee.dto.request.OrderCreateRequest;
import com.coubee.dto.request.OrderStatusUpdateRequest;
import com.coubee.dto.response.OrderCreateResponse;
import com.coubee.dto.response.OrderDetailResponse;
import com.coubee.dto.response.OrderListResponse;
import com.coubee.dto.response.OrderStatusUpdateResponse;
import org.springframework.data.domain.Pageable;

/**
 * Order Service Interface for processing order business logic
 */
public interface OrderService {

    /**
     * Create an order and prepare payment
     *
     * @param userId User ID
     * @param request Order creation request
     * @return Order creation response
     */
    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    /**
     * Get order details by order ID
     *
     * @param orderId Order ID
     * @return Order detail information
     */
    OrderDetailResponse getOrder(String orderId);

    /**
     * Get order list by user ID
     *
     * @param userId User ID
     * @param pageable Pagination information
     * @return Order list
     */
    OrderListResponse getUserOrders(Long userId, Pageable pageable);

    /**
     * Cancel an order
     *
     * @param orderId Order ID
     * @param request Order cancellation request
     * @return Cancelled order details
     */
    OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request);

    /**
     * Mark an order as received
     *
     * @param orderId Order ID
     * @return Received order details
     */
    OrderDetailResponse receiveOrder(String orderId);

    /**
     * Update order status manually
     *
     * @param orderId Order ID
     * @param request Order status update request
     * @return Updated order status information
     */
    OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request);
} 
