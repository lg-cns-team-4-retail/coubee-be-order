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
import com.coubee.coubeebeorder.domain.dto.StoreOrderSummaryResponseDto;
import com.coubee.coubeebeorder.domain.dto.UserOrderSummaryDto;
import com.coubee.coubeebeorder.domain.dto.BestsellerProductResponseDto;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderService {

    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    OrderDetailResponse getOrder(String orderId);

    OrderStatusResponse getOrderStatus(String orderId);

    Page<OrderDetailResponse> getUserOrders(Long userId, Pageable pageable, String keyword);

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

    /**
     * Get store order summary with statistics and paginated order list
     *
     * @param ownerUserId the store owner user ID
     * @param storeId the store ID
     * @param startDate start date for the summary period (optional)
     * @param endDate end date for the summary period (optional)
     * @param pageable pagination information
     * @return store order summary response containing statistics and detailed order list
     */
    StoreOrderSummaryResponseDto getStoreOrderSummary(Long ownerUserId, Long storeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // [ADD] New method signature for getting a store's orders.
    Page<OrderDetailResponse> getStoreOrders(Long ownerUserId, Long storeId, OrderStatus status, String keyword, Pageable pageable);

    /**
     * Get nearby bestseller products based on geographical coordinates
     *
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     * @param pageable pagination information
     * @return paginated list of bestseller products from nearby stores
     */
    Page<BestsellerProductResponseDto> getNearbyBestsellers(double latitude, double longitude, Pageable pageable);
}