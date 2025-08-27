package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.InvalidStatusTransitionException;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.dto.*;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.OrderTimestampRepository;
import com.coubee.coubeebeorder.kafka.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.kafka.producer.notification.event.OrderNotificationEvent;
import com.coubee.coubeebeorder.remote.product.ProductClient;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
// import io.portone.sdk.server.payment.CancelPaymentRequest; // Not available in current SDK version
import io.portone.sdk.server.payment.PaymentClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import com.coubee.coubeebeorder.remote.store.StoreResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderTimestampRepository orderTimestampRepository;
    private final ProductStockService productStockService;
    private final KafkaMessageProducer kafkaMessageProducer;
    // ✅✅✅ FeignClient 대신 공식 SDK 클라이언트를 주입받습니다. ✅✅✅
    private final PaymentClient portonePaymentClient;
    private final ProductClient productClient;
    private final StoreClient storeClient;

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
                ApiResponseDto<ProductResponseDto> productResponse = productClient.getProductById(
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

        // Record initial status history (PENDING)
        OrderTimestamp initialTimestamp = OrderTimestamp.createTimestamp(order, OrderStatus.PENDING);
        order.addStatusHistory(initialTimestamp);

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
    public OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request, Long userId, String userRole) {
        log.info("Cancelling order: {} by user: {} with role: {}", orderId, userId, userRole);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        // Determine the new cancel status based on user role
        OrderStatus newCancelStatus;
        if ("ROLE_ADMIN".equals(userRole) || "ROLE_SUPER_ADMIN".equals(userRole)) {
            newCancelStatus = OrderStatus.CANCELLED_ADMIN;
        } else if (order.getUserId().equals(userId)) {
            newCancelStatus = OrderStatus.CANCELLED_USER;
        } else {
            throw new IllegalArgumentException("주문을 취소할 권한이 없습니다.");
        }

        // Check if the order is already cancelled or received
        if (order.getStatus() == OrderStatus.CANCELLED_USER || order.getStatus() == OrderStatus.CANCELLED_ADMIN || order.getStatus() == OrderStatus.RECEIVED) {
            throw new InvalidStatusTransitionException(order.getStatus(), newCancelStatus);
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

                String cancelReason = (request != null && request.getCancelReason() != null)
                        ? request.getCancelReason()
                        : "No reason provided";

                log.warn("Payment cancellation needed but CancelPaymentRequest not available in SDK version 0.19.2");
                log.warn("Transaction ID: {}, Reason: {}", transactionId, cancelReason);

                order.getPayment().updateCancelledStatus();
                log.info("Payment cancelled successfully for order: {}", orderId);

            } catch (Exception e) {
                log.error("Error cancelling payment for order: {}", orderId, e);
                throw new ApiError("결제 취소 중 오류가 발생했습니다.");
            }
        }

        updateOrderStatusAndCreateHistory(order, newCancelStatus);

        // V3: 주문 취소 시 모든 주문 아이템의 이벤트 타입을 REFUND로 설정
        order.getItems().forEach(item -> item.updateEventType(EventType.REFUND));

        orderRepository.save(order);

        // 재고 복원 처리 - 주문 취소 시점에 재고를 복원합니다.
        log.info("재고 복원 처리 시작 - 주문 ID: {}", orderId);
        productStockService.increaseStock(order);
        log.info("재고 복원 처리 완료 - 주문 ID: {}", orderId);

        publishCancelNotificationEvent(order, newCancelStatus);

        log.info("Order cancelled successfully: {}. New status: {}", orderId, newCancelStatus);
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
    public Page<OrderDetailResponse> getUserOrders(Long userId, Pageable pageable) {
        log.info("getUserOrders 호출 - userId: {}, pageable: {}", userId, pageable);
        
        // 1. 기본 정렬된 주문 조회
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Order> orders = orderPage.getContent();
        
        log.info("1단계 - 정렬된 주문 조회 완료. 총 {}개 주문", orders.size());
        orders.forEach(order -> log.debug("정렬된 주문: orderId={}, createdAt={}", 
                order.getOrderId(), order.getCreatedAt()));

        if (orders.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. 상세 정보 포함 조회
        List<Order> ordersWithDetails = orderRepository.findWithDetailsIn(orders);
        
        log.info("2단계 - 상세 정보 조회 완료. 총 {}개 주문", ordersWithDetails.size());
        ordersWithDetails.forEach(order -> log.debug("상세 조회 후: orderId={}, createdAt={}", 
                order.getOrderId(), order.getCreatedAt()));

        // 3. 순서 보장을 위해 재정렬
        java.util.Map<Long, Order> orderMap = ordersWithDetails.stream()
                .collect(java.util.stream.Collectors.toMap(Order::getId, order -> order));

        List<Order> reorderedOrders = orders.stream()
                .map(order -> orderMap.get(order.getId()))
                .collect(java.util.stream.Collectors.toList());
                
        log.info("3단계 - 재정렬 완료. 총 {}개 주문", reorderedOrders.size());
        reorderedOrders.forEach(order -> log.debug("재정렬 후: orderId={}, createdAt={}", 
                order.getOrderId(), order.getCreatedAt()));

        // 4. 벌크 DTO 변환 (N+1 문제 해결)
        List<OrderDetailResponse> orderDetailResponses = convertToOrderDetailResponseList(reorderedOrders);

        // 5. 최종 Page 객체 생성
        Page<OrderDetailResponse> result = new PageImpl<>(orderDetailResponses, pageable, orderPage.getTotalElements());

        log.info("최종 결과 - 총 {}개 주문 반환", result.getContent().size());
        result.getContent().forEach(orderResponse -> log.debug("최종 응답: orderId={}, createdAt={}",
                orderResponse.getOrderId(), orderResponse.getCreatedAt()));

        return result;
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

        updateOrderStatusAndCreateHistory(order, OrderStatus.RECEIVED);
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

        updateOrderStatusAndCreateHistory(order, request.getStatus());
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

    @Override
    @Transactional
    public void updateOrderStatusWithHistory(String orderId, OrderStatus newStatus) {
        log.info("Updating order status with history for: {} to: {}", orderId, newStatus);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        updateOrderStatusAndCreateHistory(order, newStatus);
        orderRepository.save(order);

        log.info("Order status updated with history successfully: {}", orderId);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean isValidTransition = switch (from) {
            case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED_USER || to == OrderStatus.CANCELLED_ADMIN || to == OrderStatus.FAILED;
            case PAID -> to == OrderStatus.PREPARING || to == OrderStatus.CANCELLED_USER || to == OrderStatus.CANCELLED_ADMIN || to == OrderStatus.FAILED;
            case PREPARING -> to == OrderStatus.PREPARED || to == OrderStatus.CANCELLED_USER || to == OrderStatus.CANCELLED_ADMIN || to == OrderStatus.FAILED;
            case PREPARED -> to == OrderStatus.RECEIVED || to == OrderStatus.CANCELLED_USER || to == OrderStatus.CANCELLED_ADMIN;
            case RECEIVED, FAILED, CANCELLED_USER, CANCELLED_ADMIN -> false; // Terminal states
        };

        if (!isValidTransition) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    /**
     * Updates order status and creates a history record
     *
     * @param order the order to update
     * @param newStatus the new status
     */
    private void updateOrderStatusAndCreateHistory(Order order, OrderStatus newStatus) {
        order.updateStatus(newStatus);

        // Create and add status history record
        OrderTimestamp timestamp = OrderTimestamp.createTimestamp(order, newStatus);
        order.addStatusHistory(timestamp);

        // 상태 변경에 따른 알림 이벤트 발행
        publishOrderStatusNotificationEvent(order, newStatus);

        log.debug("Status history recorded for order {}: {}", order.getOrderId(), newStatus);
    }



    private void publishOrderStatusNotificationEvent(Order order, OrderStatus newStatus) {
        try {
            // PREPARING, PREPARED 상태에 대해서만 알림 이벤트 발행
            if (newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.PREPARED) {
                String storeName = getStoreName(order.getStoreId(), order.getUserId());
                
                OrderNotificationEvent notificationEvent = null;
                if (newStatus == OrderStatus.PREPARING) {
                    notificationEvent = OrderNotificationEvent.createPreparingNotification(
                            order.getOrderId(),
                            order.getUserId(),
                            storeName
                    );
                } else if (newStatus == OrderStatus.PREPARED) {
                    notificationEvent = OrderNotificationEvent.createPreparedNotification(
                            order.getOrderId(),
                            order.getUserId(),
                            storeName
                    );
                }
                
                if (notificationEvent != null) {
                    kafkaMessageProducer.publishOrderNotificationEvent(notificationEvent);
                    log.info("주문 상태 변경 알림 이벤트 발행 완료 - 주문: {}, 상태: {}, 매장: {}", 
                            order.getOrderId(), newStatus, storeName);
                }
            }
        } catch (Exception e) {
            log.error("주문 상태 변경 알림 이벤트 발행 실패 - 주문: {}, 상태: {}", 
                    order.getOrderId(), newStatus, e);
        }
    }

    private void publishCancelNotificationEvent(Order order, OrderStatus cancelStatus) {
        try {
            String storeName = getStoreName(order.getStoreId(), order.getUserId());
            
            OrderNotificationEvent notificationEvent = null;
            if (cancelStatus == OrderStatus.CANCELLED_USER) {
                notificationEvent = OrderNotificationEvent.createCancelledUserNotification(
                        order.getOrderId(),
                        order.getUserId(),
                        storeName
                );
            } else if (cancelStatus == OrderStatus.CANCELLED_ADMIN) {
                notificationEvent = OrderNotificationEvent.createCancelledAdminNotification(
                        order.getOrderId(),
                        order.getUserId(),
                        storeName
                );
            }
            
            if (notificationEvent != null) {
                kafkaMessageProducer.publishOrderNotificationEvent(notificationEvent);
                log.info("주문 취소 알림 이벤트 발행 완료 - 주문: {}, 상태: {}, 매장: {}", 
                        order.getOrderId(), cancelStatus, storeName);
            }
        } catch (Exception e) {
            log.error("주문 취소 알림 이벤트 발행 실패 - 주문: {}, 상태: {}", 
                    order.getOrderId(), cancelStatus, e);
        }
    }

    private String getStoreName(Long storeId, Long userId) {
        try {
            return storeClient.getStoreNameById(storeId, userId).getData();
        } catch (Exception e) {
            log.warn("매장명 조회 실패 - storeId: {}, userId: {}, 기본값 사용", storeId, userId, e);
            return "매장";
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

    /**
     * 주문 목록을 벌크 API 호출을 사용하여 OrderDetailResponse 목록으로 변환
     * N+1 문제를 해결하기 위해 모든 필요한 데이터를 한 번에 조회합니다.
     */
    private List<OrderDetailResponse> convertToOrderDetailResponseList(List<Order> orders) {
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 모든 고유한 storeId와 productId 수집
        Set<Long> storeIds = orders.stream()
                .map(Order::getStoreId)
                .collect(Collectors.toSet());

        Set<Long> productIds = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());

        // 2. 벌크 API 호출로 모든 데이터를 한 번에 조회
        Map<Long, StoreResponseDto> storeMap = getBulkStoreData(storeIds, orders.get(0).getUserId());
        Map<Long, ProductResponseDto> productMap = getBulkProductData(productIds, orders.get(0).getUserId());

        // 3. 조회된 데이터를 사용하여 OrderDetailResponse 목록 생성
        return orders.stream()
                .map(order -> convertToOrderDetailResponseWithMaps(order, storeMap, productMap))
                .collect(Collectors.toList());
    }

    /**
     * 벌크 스토어 데이터 조회 (Circuit Breaker 적용)
     */
    @CircuitBreaker(name = "downstreamServices", fallbackMethod = "getBulkStoreDataFallback")
    private Map<Long, StoreResponseDto> getBulkStoreData(Set<Long> storeIds, Long userId) {
        if (storeIds.isEmpty()) {
            return Map.of();
        }

        List<Long> storeIdList = new ArrayList<>(storeIds);
        ApiResponseDto<Map<Long, StoreResponseDto>> response = storeClient.getStoresByIds(storeIdList, userId);
        return response.getData() != null ? response.getData() : Map.of();
    }

    /**
     * 벌크 상품 데이터 조회 (Circuit Breaker 적용)
     */
    @CircuitBreaker(name = "downstreamServices", fallbackMethod = "getBulkProductDataFallback")
    private Map<Long, ProductResponseDto> getBulkProductData(Set<Long> productIds, Long userId) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<Long> productIdList = new ArrayList<>(productIds);
        ApiResponseDto<Map<Long, ProductResponseDto>> response = productClient.getProductsByIds(productIdList, userId);
        return response.getData() != null ? response.getData() : Map.of();
    }

    /**
     * 미리 조회된 맵 데이터를 사용하여 OrderDetailResponse 생성
     */
    private OrderDetailResponse convertToOrderDetailResponseWithMaps(Order order,
                                                                     Map<Long, StoreResponseDto> storeMap,
                                                                     Map<Long, ProductResponseDto> productMap) {
        // 스토어 정보 조회 (폴백 데이터 사용 가능)
        StoreResponseDto storeDetails = storeMap.get(order.getStoreId());
        if (storeDetails == null) {
            log.warn("Store data not found for storeId: {}. Using fallback.", order.getStoreId());
            storeDetails = createFallbackStoreData(order.getStoreId());
        }

        // 주문 아이템 응답 생성
        List<OrderDetailResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    ProductResponseDto productDetails = productMap.get(item.getProductId());
                    if (productDetails == null) {
                        log.warn("Product data not found for productId: {}. Using fallback.", item.getProductId());
                        productDetails = createFallbackProductData(item.getProductId());
                    }

                    return OrderDetailResponse.OrderItemResponse.builder()
                            .product(productDetails)
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .totalPrice(item.getTotalPrice())
                            .eventType(item.getEventType() != null ? item.getEventType().name() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .store(storeDetails)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .orderToken(order.getOrderToken())
                .orderQR(order.getOrderQR())
                .paidAtUnix(order.getPaidAtUnix())
                .items(itemResponses)
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

    private OrderDetailResponse convertToOrderDetailResponse(Order order) {
        // Fetch store details with circuit breaker
        StoreResponseDto storeDetails = getStoreDetailsWithCircuitBreaker(order.getStoreId(), order.getUserId());

        // Fetch product details for each item with circuit breaker
        List<OrderDetailResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    ProductResponseDto productDetails = getProductDetailsWithCircuitBreaker(item.getProductId(), order.getUserId());

                    return OrderDetailResponse.OrderItemResponse.builder()
                            .product(productDetails) // Full product details
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .totalPrice(item.getTotalPrice())
                            .eventType(item.getEventType() != null ? item.getEventType().name() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .store(storeDetails) // Full store details
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .orderToken(order.getOrderToken())
                .orderQR(order.getOrderQR())
                .paidAtUnix(order.getPaidAtUnix())
                .items(itemResponses)
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

    /**
     * Circuit breaker로 보호되는 스토어 정보 조회
     */
    @CircuitBreaker(name = "downstreamServices", fallbackMethod = "getStoreDetailsFallback")
    private StoreResponseDto getStoreDetailsWithCircuitBreaker(Long storeId, Long userId) {
        ApiResponseDto<StoreResponseDto> storeResponse = storeClient.getStoreById(storeId, userId);
        return storeResponse.getData();
    }

    /**
     * Circuit breaker로 보호되는 상품 정보 조회
     */
    @CircuitBreaker(name = "downstreamServices", fallbackMethod = "getProductDetailsFallback")
    private ProductResponseDto getProductDetailsWithCircuitBreaker(Long productId, Long userId) {
        ApiResponseDto<ProductResponseDto> productResponse = productClient.getProductById(productId, userId);
        return productResponse.getData();
    }

    /**
     * 스토어 정보 조회 폴백 메서드
     */
    private StoreResponseDto getStoreDetailsFallback(Long storeId, Long userId, Exception ex) {
        log.warn("Circuit breaker activated for store details - Store ID: {}, User ID: {}. Using fallback data.", storeId, userId, ex);

        StoreResponseDto fallbackStore = new StoreResponseDto();
        fallbackStore.setStoreId(storeId);
        fallbackStore.setStoreName("매장 정보 일시 불가");
        fallbackStore.setDescription("매장 정보를 불러올 수 없습니다.");
        fallbackStore.setStoreAddress("주소 정보 없음");
        fallbackStore.setContactNo("연락처 정보 없음");
        fallbackStore.setWorkingHour("영업시간 정보 없음");
        // storeTag는 List<CategoryDto>이므로 빈 리스트로 설정
        fallbackStore.setStoreTag(new ArrayList<>());

        return fallbackStore;
    }

    /**
     * 상품 정보 조회 폴백 메서드
     */
    private ProductResponseDto getProductDetailsFallback(Long productId, Long userId, Exception ex) {
        log.warn("Circuit breaker activated for product details - Product ID: {}, User ID: {}. Using fallback data.", productId, userId, ex);

        ProductResponseDto fallbackProduct = new ProductResponseDto();
        fallbackProduct.setProductId(productId);
        fallbackProduct.setProductName("상품 정보 일시 불가");
        fallbackProduct.setDescription("상품 정보를 불러올 수 없습니다.");
        fallbackProduct.setOriginPrice(0);
        fallbackProduct.setSalePrice(0);
        fallbackProduct.setStock(0);

        return fallbackProduct;
    }

    /**
     * 벌크 스토어 데이터 조회 폴백 메서드
     */
    private Map<Long, StoreResponseDto> getBulkStoreDataFallback(Set<Long> storeIds, Long userId, Exception ex) {
        log.warn("Circuit breaker activated for bulk store data - Store IDs: {}, User ID: {}. Using fallback data.", storeIds, userId, ex);

        return storeIds.stream()
                .collect(Collectors.toMap(
                        storeId -> storeId,
                        this::createFallbackStoreData
                ));
    }

    /**
     * 벌크 상품 데이터 조회 폴백 메서드
     */
    private Map<Long, ProductResponseDto> getBulkProductDataFallback(Set<Long> productIds, Long userId, Exception ex) {
        log.warn("Circuit breaker activated for bulk product data - Product IDs: {}, User ID: {}. Using fallback data.", productIds, userId, ex);

        return productIds.stream()
                .collect(Collectors.toMap(
                        productId -> productId,
                        this::createFallbackProductData
                ));
    }

    /**
     * 폴백 스토어 데이터 생성
     */
    private StoreResponseDto createFallbackStoreData(Long storeId) {
        StoreResponseDto fallbackStore = new StoreResponseDto();
        fallbackStore.setStoreId(storeId);
        fallbackStore.setStoreName("매장 정보 일시 불가");
        fallbackStore.setDescription("매장 정보를 불러올 수 없습니다.");
        fallbackStore.setStoreAddress("주소 정보 없음");
        fallbackStore.setContactNo("연락처 정보 없음");
        fallbackStore.setWorkingHour("영업시간 정보 없음");
        // storeTag는 List<CategoryDto>이므로 빈 리스트로 설정
        fallbackStore.setStoreTag(new ArrayList<>());
        return fallbackStore;
    }

    /**
     * 폴백 상품 데이터 생성
     */
    private ProductResponseDto createFallbackProductData(Long productId) {
        ProductResponseDto fallbackProduct = new ProductResponseDto();
        fallbackProduct.setProductId(productId);
        fallbackProduct.setProductName("상품 정보 일시 불가");
        fallbackProduct.setDescription("상품 정보를 불러올 수 없습니다.");
        fallbackProduct.setOriginPrice(0);
        fallbackProduct.setSalePrice(0);
        fallbackProduct.setStock(0);
        return fallbackProduct;
    }
}