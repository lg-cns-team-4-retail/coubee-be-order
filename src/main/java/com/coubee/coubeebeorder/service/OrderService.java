package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.dto.OrderCancelRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateResponse;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderListResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateResponse;
import com.coubee.coubeebeorder.domain.dto.UserOrderSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    OrderDetailResponse getOrder(String orderId);

    OrderStatusResponse getOrderStatus(String orderId);

    Page<OrderDetailResponse> getUserOrders(Long userId, Pageable pageable);

    OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request, Long userId, String userRole);

    OrderDetailResponse receiveOrder(String orderId);

    OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request, Long userId);

    /**
     * Updates order status with history tracking (for internal service use)
     *
     * @param orderId the order ID
     * @param newStatus the new status
     */
    void updateOrderStatusWithHistory(String orderId, OrderStatus newStatus);

    /**
     * Get user order summary aggregation (for backend services)
     *
     * @param userId the user ID
     * @return user order summary containing aggregated statistics
     */
    UserOrderSummaryDto getUserOrderSummary(Long userId);
}