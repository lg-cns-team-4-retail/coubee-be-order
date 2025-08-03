package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.dto.*;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        log.info("Creating order for user: {}", userId);
        
        // TODO: Implement order creation logic
        String orderId = "order_" + java.util.UUID.randomUUID().toString().replace("-", "");
        
        return OrderCreateResponse.builder()
                .orderId(orderId)
                .paymentId(orderId)
                .amount(1000) // Placeholder
                .orderName("Test Order")
                .buyerName(request.getRecipientName())
                .build();
    }

    @Override
    public OrderDetailResponse getOrder(String orderId) {
        log.info("Getting order details for: {}", orderId);

        // TODO: Implement order retrieval logic
        throw new NotFound("주문을 찾을 수 없습니다.");
    }

    @Override
    public OrderStatusResponse getOrderStatus(String orderId) {
        log.info("Getting order status for: {}", orderId);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        return OrderStatusResponse.builder()
                .orderId(orderId)
                .status(order.getStatus())
                .build();
    }

    @Override
    public OrderListResponse getUserOrders(Long userId, Pageable pageable) {
        log.info("Getting orders for user: {}", userId);
        
        // TODO: Implement user orders retrieval logic
        return OrderListResponse.builder()
                .orders(java.util.Collections.emptyList())
                .pageInfo(OrderListResponse.PageInfo.builder()
                        .page(0)
                        .size(10)
                        .totalPages(0)
                        .totalElements(0)
                        .first(true)
                        .last(true)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request) {
        log.info("Cancelling order: {} with reason: {}", orderId, request.getCancelReason());
        
        // TODO: Implement order cancellation logic
        throw new NotFound("주문을 찾을 수 없습니다.");
    }

    @Override
    @Transactional
    public OrderDetailResponse receiveOrder(String orderId) {
        log.info("Marking order as received: {}", orderId);
        
        // TODO: Implement order receive logic
        throw new NotFound("주문을 찾을 수 없습니다.");
    }

    @Override
    @Transactional
    public OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request, Long userId) {
        log.info("Updating order status for: {} to: {}", orderId, request.getStatus());

        // Find the order
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        // Store previous status
        OrderStatus previousStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(previousStatus, request.getStatus());

        // Update order status
        order.updateStatus(request.getStatus());
        orderRepository.save(order);

        log.info("Order status updated successfully: {} -> {}", previousStatus, request.getStatus());

        return OrderStatusUpdateResponse.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .currentStatus(request.getStatus())
                .reason(request.getReason())
                .updatedAt(java.time.LocalDateTime.now())
                .updatedByUserId(userId)
                .build();
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Define valid transitions
        boolean isValidTransition = switch (from) {
            case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED || to == OrderStatus.FAILED;
            case PAID -> to == OrderStatus.PREPARING || to == OrderStatus.CANCELLED || to == OrderStatus.FAILED;
            case PREPARING -> to == OrderStatus.PREPARED || to == OrderStatus.CANCELLED || to == OrderStatus.FAILED;
            case PREPARED -> to == OrderStatus.RECEIVED || to == OrderStatus.CANCELLED;
            case RECEIVED, CANCELLED, FAILED -> false; // Terminal states
        };

        if (!isValidTransition) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}