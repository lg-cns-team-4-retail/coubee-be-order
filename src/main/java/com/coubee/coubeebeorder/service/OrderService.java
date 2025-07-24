package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.dto.OrderCancelRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateResponse;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderListResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    OrderDetailResponse getOrder(String orderId);

    OrderListResponse getUserOrders(Long userId, Pageable pageable);

    OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request);

    OrderDetailResponse receiveOrder(String orderId);

    OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request);
}