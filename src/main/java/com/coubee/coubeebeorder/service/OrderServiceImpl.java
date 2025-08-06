package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.dto.*;
import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.PaymentRepository;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.InvalidStatusTransitionException;
import com.coubee.coubeebeorder.event.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.domain.event.OrderEvent;
import com.coubee.coubeebeorder.remote.PortOneClient;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentCancelRequest;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentCancelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final PortOneClient portOneClient;

    @Override
    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        log.info("Creating order for user: {}", userId);

        // Generate unique order ID
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");
        
        // Calculate total amount from items (placeholder prices since no Product Service integration)
        int totalAmount = request.getItems().stream()
                .mapToInt(item -> item.getQuantity() * 1000) // Using 1000 as placeholder price per item
                .sum();
        
        // Create Order entity
        Order order = Order.createOrder(
                orderId,
                userId,
                request.getStoreId(),
                totalAmount,
                request.getRecipientName()
        );
        
        // Add order items
        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            OrderItem orderItem = OrderItem.createOrderItem(
                    itemRequest.getProductId(),
                    "Product " + itemRequest.getProductId(), // Placeholder product name
                    itemRequest.getQuantity(),
                    1000 // Placeholder price
            );
            order.addOrderItem(orderItem);
        }
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Generate order name
        String orderName = generateOrderName(savedOrder.getItems());
        
        log.info("Order created successfully: orderId={}, totalAmount={}", orderId, totalAmount);
        
        return OrderCreateResponse.builder()
                .orderId(orderId)
                .paymentId(orderId) // Using orderId as paymentId
                .amount(totalAmount)
                .orderName(orderName)
                .buyerName(request.getRecipientName())
                .build();
    }

    @Override
    public OrderDetailResponse getOrder(String orderId) {
        log.info("Getting order details for: {}", orderId);

        // Find order by orderId
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));
        
        // Convert to response DTO
        return convertToOrderDetailResponse(order);
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
        
        // Get user orders with pagination
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        // Convert to OrderSummary DTOs
        List<OrderListResponse.OrderSummary> orderSummaries = orderPage.getContent().stream()
                .map(this::convertToOrderSummary)
                .collect(Collectors.toList());
        
        // Create PageInfo
        OrderListResponse.PageInfo pageInfo = OrderListResponse.PageInfo.builder()
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalPages(orderPage.getTotalPages())
                .totalElements((int) orderPage.getTotalElements())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();
        
        return OrderListResponse.builder()
                .orders(orderSummaries)
                .pageInfo(pageInfo)
                .build();
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request) {
        log.info("Cancelling order: {} with reason: {}", orderId, request.getCancelReason());
        
        // Find order by orderId
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));
        
        // Validate order status for cancellation
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RECEIVED) {
            throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.CANCELLED);
        }
        
        // If order has payment, cancel it through PortOne
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID) {
            try {
                PortOnePaymentCancelRequest cancelRequest = PortOnePaymentCancelRequest.builder()
                        .impUid(order.getPayment().getPgTransactionId())
                        .merchantUid(orderId)
                        .amount(order.getTotalAmount())
                        .reason(request.getCancelReason())
                        .build();
                
                PortOnePaymentCancelResponse cancelResponse = portOneClient.cancelPayment(cancelRequest);
                
                if (cancelResponse.isSuccess()) {
                    order.getPayment().updateCancelledStatus();
                    log.info("Payment cancelled successfully for order: {}", orderId);
                } else {
                    log.error("Payment cancellation failed for order: {}, reason: {}", orderId, cancelResponse.getMessage());
                    throw new ApiError("결제 취소에 실패했습니다: " + cancelResponse.getMessage());
                }
            } catch (Exception e) {
                log.error("Error cancelling payment for order: {}", orderId, e);
                throw new ApiError("결제 취소 중 오류가 발생했습니다.");
            }
        }
        
        // Update order status to CANCELLED
        order.updateStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        
        // Publish stock increase event to Kafka
        try {
            OrderEvent stockIncreaseEvent = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("STOCK_INCREASE")
                    .orderId(orderId)
                    .userId(order.getUserId())
                    .storeId(order.getStoreId())
                    .items(order.getItems().stream()
                            .map(item -> OrderEvent.OrderItemEvent.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            
            kafkaMessageProducer.publishOrderEvent(stockIncreaseEvent);
            log.info("Stock increase event published for cancelled order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish stock increase event for order: {}", orderId, e);
            // Don't fail the cancellation if event publishing fails
        }
        
        log.info("Order cancelled successfully: {}", orderId);
        return convertToOrderDetailResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderDetailResponse receiveOrder(String orderId) {
        log.info("Marking order as received: {}", orderId);
        
        // Find order by orderId
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));
        
        // Validate order status - only PREPARED orders can be received
        if (order.getStatus() != OrderStatus.PREPARED) {
            throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.RECEIVED);
        }
        
        // Update order status to RECEIVED
        order.updateStatus(OrderStatus.RECEIVED);
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order marked as received successfully: {}", orderId);
        return convertToOrderDetailResponse(savedOrder);
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
    
    private String generateOrderName(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "빈 주문";
        }
        
        OrderItem firstItem = items.get(0);
        if (items.size() == 1) {
            return firstItem.getProductName();
        } else {
            return firstItem.getProductName() + " 외 " + (items.size() - 1) + "건";
        }
    }
    
    private OrderDetailResponse convertToOrderDetailResponse(Order order) {
        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .orderToken(order.getOrderToken())
                .orderQR(order.getOrderQR())
                .items(order.getItems().stream()
                        .map(item -> OrderDetailResponse.OrderItemResponse.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .payment(order.getPayment() != null ? 
                        OrderDetailResponse.PaymentResponse.builder()
                                .paymentId(order.getPayment().getPaymentId())
                                .method(order.getPayment().getMethod())
                                .amount(order.getPayment().getAmount())
                                .status(order.getPayment().getStatus())
                                .paidAt(order.getPayment().getPaidAt())
                                .receiptUrl(order.getPayment().getReceiptUrl())
                                .build() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    private OrderListResponse.OrderSummary convertToOrderSummary(Order order) {
        return OrderListResponse.OrderSummary.builder()
                .orderId(order.getOrderId())
                .storeId(order.getStoreId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .orderName(generateOrderName(order.getItems()))
                .createdAt(order.getCreatedAt())
                .build();
    }
}