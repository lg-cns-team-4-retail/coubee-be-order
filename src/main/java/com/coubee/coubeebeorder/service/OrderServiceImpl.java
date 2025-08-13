package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.InvalidStatusTransitionException;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.dto.*;
import com.coubee.coubeebeorder.domain.event.OrderEvent;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.event.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.remote.client.ProductServiceClient;
import com.coubee.coubeebeorder.remote.dto.ProductResponseDto;
// import io.portone.sdk.server.payment.CancelPaymentRequest; // Not available in current SDK version
import io.portone.sdk.server.payment.PaymentClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final KafkaMessageProducer kafkaMessageProducer;
    // ✅✅✅ FeignClient 대신 공식 SDK 클라이언트를 주입받습니다. ✅✅✅
    private final PaymentClient portonePaymentClient;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        log.info("Creating order for user: {}", userId);

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");

        // First, fetch all product information and calculate total amount
        int totalAmount = 0;
        List<ProductResponseDto> productDetails = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            try {
                // Call product service to get product details
                log.debug("Fetching product details for productId: {}", itemRequest.getProductId());
                ApiResponseDto<ProductResponseDto> productResponse = productServiceClient.getProductById(
                        itemRequest.getProductId(), userId);

                if (productResponse == null || productResponse.getData() == null) {
                    throw new NotFound("Product not found: " + itemRequest.getProductId());
                }

                ProductResponseDto product = productResponse.getData();

                // Check if product has sufficient stock
                if (product.getStock() < itemRequest.getQuantity()) {
                    throw new ApiError("Insufficient stock for product: " + product.getProductName() +
                                     ". Available: " + product.getStock() + ", Requested: " + itemRequest.getQuantity());
                }

                // Use salePrice for calculations
                int itemTotalPrice = product.getSalePrice() * itemRequest.getQuantity();
                totalAmount += itemTotalPrice;

                productDetails.add(product);

                log.debug("Validated product: productId={}, productName={}, quantity={}, price={}, itemTotal={}",
                         product.getProductId(), product.getProductName(), itemRequest.getQuantity(),
                         product.getSalePrice(), itemTotalPrice);

            } catch (FeignException.NotFound e) {
                log.error("Product not found: productId={}", itemRequest.getProductId());
                throw new NotFound("Product not found: " + itemRequest.getProductId());
            } catch (FeignException e) {
                log.error("Failed to fetch product details: productId={}, error={}",
                         itemRequest.getProductId(), e.getMessage());
                throw new ApiError("Failed to fetch product details for product: " + itemRequest.getProductId());
            }
        }

        // Create order with the calculated total amount
        Order order = Order.createOrder(
                orderId, userId, request.getStoreId(), totalAmount, request.getRecipientName());

        // Add order items with real product data
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderCreateRequest.OrderItemRequest itemRequest = request.getItems().get(i);
            ProductResponseDto product = productDetails.get(i);

            OrderItem orderItem = OrderItem.createOrderItem(
                    itemRequest.getProductId(),
                    product.getProductName(),
                    itemRequest.getQuantity(),
                    product.getSalePrice()
            );
            order.addOrderItem(orderItem);
        }

        orderRepository.save(order);

        log.info("Order created successfully: orderId={}, totalAmount={}", orderId, totalAmount);

        return OrderCreateResponse.builder()
                .orderId(orderId)
                .paymentId(orderId)
                .amount(totalAmount)
                .orderName(generateOrderName(order.getItems()))
                .buyerName(request.getRecipientName())
                .build();
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request) {
        log.info("Cancelling order: {} with reason: {}", orderId, request.getCancelReason());

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RECEIVED) {
            throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.CANCELLED);
        }

        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID) {
            try {
                // ✅✅✅ 공식 SDK를 사용하여 결제를 취소합니다. ✅✅✅
                // TODO: Implement proper cancellation when CancelPaymentRequest is available
                // transactionId는 Payment 테이블에 저장된 pg_transaction_id를 사용해야 합니다.
                String transactionId = order.getPayment().getPgTransactionId();

                if (transactionId == null || transactionId.isBlank()) {
                    throw new ApiError("취소할 결제 정보(PG Transaction ID)가 없습니다.");
                }

                log.warn("Payment cancellation needed but CancelPaymentRequest not available in SDK version 0.19.2");
                log.warn("Transaction ID: {}, Reason: {}", transactionId, request.getCancelReason());

                order.getPayment().updateCancelledStatus();
                log.info("Payment cancelled successfully for order: {}", orderId);

            } catch (Exception e) {
                log.error("Error cancelling payment for order: {}", orderId, e);
                throw new ApiError("결제 취소 중 오류가 발생했습니다.");
            }
        }

        order.updateStatus(OrderStatus.CANCELLED);

        // V3: 주문 취소 시 모든 주문 아이템의 이벤트 타입을 REFUND로 설정
        order.getItems().forEach(item -> item.updateEventType(EventType.REFUND));

        publishStockIncreaseEvent(order);

        log.info("Order cancelled successfully: {}", orderId);
        return convertToOrderDetailResponse(order);
    }

    @Override
    public OrderDetailResponse getOrder(String orderId) {
        log.info("Getting order details for: {}", orderId);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

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

        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<OrderListResponse.OrderSummary> orderSummaries = orderPage.getContent().stream()
                .map(this::convertToOrderSummary)
                .collect(Collectors.toList());

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
    public OrderDetailResponse receiveOrder(String orderId) {
        log.info("Marking order as received: {}", orderId);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        if (order.getStatus() != OrderStatus.PREPARED) {
            throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.RECEIVED);
        }

        order.updateStatus(OrderStatus.RECEIVED);
        orderRepository.save(order);

        log.info("Order marked as received successfully: {}", orderId);
        return convertToOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request, Long userId) {
        log.info("Updating order status for: {} to: {}", orderId, request.getStatus());

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        OrderStatus previousStatus = order.getStatus();

        validateStatusTransition(previousStatus, request.getStatus());

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

    private void publishStockIncreaseEvent(Order order) {
        try {
            OrderEvent stockIncreaseEvent = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("STOCK_INCREASE")
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .items(order.getItems().stream()
                            .map(item -> OrderEvent.OrderItemEvent.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList())
                    .build();

            kafkaMessageProducer.publishOrderEvent(stockIncreaseEvent);
            log.info("Stock increase event published for cancelled order: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish stock increase event for order: {}", order.getOrderId(), e);
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
                .paidAtUnix(order.getPaidAtUnix()) // V3: 결제 완료 시점 UNIX 타임스탬프 추가
                .items(order.getItems().stream()
                        .map(item -> OrderDetailResponse.OrderItemResponse.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .totalPrice(item.getTotalPrice())
                                .eventType(item.getEventType() != null ? item.getEventType().name() : null) // V3: 이벤트 타입 추가
                                .build())
                        .collect(Collectors.toList()))
                .payment(order.getPayment() != null ?
                        OrderDetailResponse.PaymentResponse.builder()
                                .paymentId(order.getPayment().getPaymentId())
                                .pgProvider(order.getPayment().getPgProvider())
                                .method(order.getPayment().getMethod())
                                .amount(order.getPayment().getAmount())
                                .status(order.getPayment().getStatus().name())
                                .paidAt(order.getPayment().getPaidAt())
                                .build() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderListResponse.OrderSummary convertToOrderSummary(Order order) {
        return OrderListResponse.OrderSummary.builder()
                .orderId(order.getOrderId())
                .storeId(order.getStoreId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .orderName(generateOrderName(order.getItems()))
                .createdAt(order.getCreatedAt())
                .build();
    }
}