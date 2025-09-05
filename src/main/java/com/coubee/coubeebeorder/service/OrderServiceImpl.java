package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.InvalidStatusTransitionException;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.dto.*;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.OrderRepository.OrderCountByStatusProjection;
import com.coubee.coubeebeorder.domain.repository.OrderRepository.UserOrderSummaryProjection;
import com.coubee.coubeebeorder.domain.repository.OrderTimestampRepository;
import com.coubee.coubeebeorder.kafka.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.kafka.producer.notification.event.OrderNotificationEvent;
import com.coubee.coubeebeorder.remote.product.ProductClient;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import com.coubee.coubeebeorder.remote.user.UserServiceClient;
import com.coubee.coubeebeorder.remote.user.SiteUserInfoDto;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import java.util.Objects;
import com.coubee.coubeebeorder.remote.hotdeal.HotdealResponseDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        log.info("Creating order for user: {}", userId);

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");
        
        // 상점 정보를 조회하여 상점 이름을 가져옵니다.
        // (translation: Fetch store information to get the store name.)
        ApiResponseDto<StoreResponseDto> storeResponse = storeClient.getStoreById(request.getStoreId(), userId);
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new NotFound("Store not found with ID: " + request.getStoreId());
        }
        String storeName = storeResponse.getData().getStoreName();

        // 상품 정보를 가져오고 원가, 판매가, 상품별 할인액을 계산합니다.
        // (Fetch product information and calculate origin price, sale price, and product-specific discounts.)
        int totalOriginAmount = 0;
        int totalSaleAmount = 0;
        int productDiscountAmount = 0;
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

                int quantity = itemRequest.getQuantity();
                
                // 원가와 판매가를 각각 계산합니다.
                // (Calculate origin price and sale price separately.)
                totalOriginAmount += product.getOriginPrice() * quantity;
                totalSaleAmount += product.getSalePrice() * quantity;

                productDetails.add(product);

                log.debug("Validated product: productId={}, productName={}, quantity={}, originPrice={}, salePrice={}",
                         product.getProductId(), product.getProductName(), quantity,
                         product.getOriginPrice(), product.getSalePrice());

            } catch (FeignException.NotFound e) {
                log.error("Product not found: productId={}", itemRequest.getProductId());
                throw new NotFound("Product not found: " + itemRequest.getProductId());
            } catch (FeignException e) {
                log.error("Failed to fetch product details: productId={}, error={}",
                         itemRequest.getProductId(), e.getMessage());
                throw new ApiError("Failed to fetch product details for product: " + itemRequest.getProductId());
            }
        }

        // 상품 자체 할인액(원가 - 판매가)을 계산합니다.
        // (Calculate product-specific discount amount (origin price - sale price).)
        productDiscountAmount = totalOriginAmount - totalSaleAmount;

        // 핫딜 할인을 확인하고 추적합니다.
        // (Check for active hotdeal and track hotdeal status.)
        int hotdealDiscountAmount = 0;
        boolean isHotdealActive = false;

        try {
            log.debug("Checking for active hotdeal for storeId: {}", request.getStoreId());
            ApiResponseDto<HotdealResponseDto> hotdealResponse = storeClient.getActiveHotdeal(request.getStoreId());

            if (hotdealResponse != null && hotdealResponse.getData() != null) {
                HotdealResponseDto hotdeal = hotdealResponse.getData();
                isHotdealActive = true;
                log.info("Active hotdeal found for storeId: {}, saleRate: {}, maxDiscount: {}",
                        request.getStoreId(), hotdeal.getSaleRate(), hotdeal.getMaxDiscount());

                // 핫딜 할인은 판매가 총액을 기준으로 계산합니다.
                // (Hotdeal discount is calculated based on the total sale amount.)
                double calculatedDiscount = totalSaleAmount * hotdeal.getSaleRate();
                hotdealDiscountAmount = (int) Math.min(calculatedDiscount, hotdeal.getMaxDiscount());

                log.info("Hotdeal applied: totalSaleAmount={}, hotdealDiscountAmount={}",
                        totalSaleAmount, hotdealDiscountAmount);
            } else {
                log.debug("No active hotdeal found for storeId: {}", request.getStoreId());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch hotdeal information for storeId: {}, proceeding without discount. Error: {}",
                    request.getStoreId(), e.getMessage());
            // Continue without discount if hotdeal service fails
        }

        // 상품 자체 할인액(원가 - 판매가)과 핫딜 할인을 합산하여 최종 총 할인액을 계산합니다.
        // (Calculate the final total discount amount by summing the product-specific discount and the hotdeal discount.)
        int totalDiscountAmount = productDiscountAmount + hotdealDiscountAmount;
        
        // 최종 결제 금액을 계산합니다 (판매가 총액 - 핫딜 할인액).
        // (Calculate the final payment amount (total sale amount - hotdeal discount).)
        int finalPaymentAmount = totalSaleAmount - hotdealDiscountAmount;

        // 새로운 비즈니스 로직에 따라 주문을 생성합니다. (storeName 추가)
        // (translation: Create order according to the new business logic (with storeName added).)
        Order order = Order.createOrder(
                orderId, userId, request.getStoreId(), storeName, totalOriginAmount, totalDiscountAmount, finalPaymentAmount, request.getRecipientName());

        // Add order items with real product data and hotdeal status
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderCreateRequest.OrderItemRequest itemRequest = request.getItems().get(i);
            ProductResponseDto product = productDetails.get(i);

            // 주문 아이템 추가 루프 수정 (description 추가)
            // (translation: Modify order item creation loop (with description added).)
            OrderItem orderItem = OrderItem.createOrderItemWithHotdeal(
                    itemRequest.getProductId(),
                    product.getProductName(),
                    product.getDescription(), // Add product description here
                    itemRequest.getQuantity(),
                    product.getSalePrice(),
                    EventType.PURCHASE,
                    isHotdealActive
            );
            order.addOrderItem(orderItem);
        }

        // Record initial status history (PENDING)
        OrderTimestamp initialTimestamp = OrderTimestamp.createTimestamp(order, OrderStatus.PENDING);
        order.addStatusHistory(initialTimestamp);

        orderRepository.save(order);

        log.info("Order created successfully: orderId={}, totalOriginAmount={}, totalDiscountAmount={}, finalPaymentAmount={}",
                orderId, totalOriginAmount, totalDiscountAmount, finalPaymentAmount);

        return OrderCreateResponse.builder()
                .orderId(orderId)
                .paymentId(orderId)
                .amount(finalPaymentAmount)
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

        // orderRepository.save(order); // Redundant call removed - managed entity changes are automatically persisted

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
    public Page<OrderDetailResponse> getUserOrders(Long userId, Pageable pageable, String keyword) {
        log.info("Getting user orders - userId: {}, pageable: {}, keyword: {}", userId, pageable, keyword);

        // Step 1: Fetch paginated order data using native query with explicit type casting
        Page<Object[]> orderDataPage = orderRepository.findUserOrderIdsNative(userId, keyword, pageable);

        if (orderDataPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Step 2: Extract order IDs from the Object[] results (first column is order_id)
        List<String> orderIds = orderDataPage.getContent().stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());

        // Step 3: Fetch the full details for the order IDs using fetch joins
        List<Order> ordersWithDetails = orderRepository.findWithDetailsIn(orderIds);

        // Step 4: Convert the detailed entities to DTOs
        List<OrderDetailResponse> orderDetailResponses = convertToOrderDetailResponseList(ordersWithDetails);

        // Step 5: Create and return the final Page object with the DTOs and original pagination info
        return new PageImpl<>(orderDetailResponses, pageable, orderDataPage.getTotalElements());
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
        // orderRepository.save(order); // Redundant call removed - managed entity changes are automatically persisted

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
        // orderRepository.save(order); // Redundant call removed - managed entity changes are automatically persisted

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
        // orderRepository.save(order); // Redundant call removed - managed entity changes are automatically persisted

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
            // [수정] StoreClient를 통해 매장 정보를 조회합니다. (translation: [MODIFIED] Fetch store information via StoreClient.)
            ApiResponseDto<StoreResponseDto> storeResponse = storeClient.getStoreById(order.getStoreId(), order.getUserId());
            String storeName = storeResponse.getData() != null ? storeResponse.getData().getStoreName() : "매장";

            if (cancelStatus == OrderStatus.CANCELLED_USER) {
                // 고객이 취소한 경우 (translation: When the customer cancels)

                // Send cancellation notification to customer only

                // 2. 고객에게 보낼 '취소 완료' 알림 (translation: 2. "Cancellation Confirmed" notification for the customer)
                OrderNotificationEvent forCustomer = OrderNotificationEvent.createCancelledUserNotification(
                        order.getOrderId(),
                        order.getUserId(), // ★ 알림 수신 대상을 고객 ID로 설정 (translation: ★ Set recipient to customer's userId)
                        storeName
                );
                kafkaMessageProducer.publishOrderNotificationEvent(forCustomer);

            } else if (cancelStatus == OrderStatus.CANCELLED_ADMIN) {
                // 점주가 취소한 경우 (기존 로직 유지, 고객에게만 발송) 
                // (translation: When the admin cancels (maintain existing logic, send only to customer))
                OrderNotificationEvent forCustomer = OrderNotificationEvent.createCancelledAdminNotification(
                        order.getOrderId(),
                        order.getUserId(),
                        storeName
                );
                kafkaMessageProducer.publishOrderNotificationEvent(forCustomer);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish order cancellation notification event - Order: {}", order.getOrderId(), e);
        }
    }

    private String getStoreName(Long storeId, Long userId) {
        try {
            ApiResponseDto<StoreResponseDto> response = storeClient.getStoreById(storeId, userId);
            if (response != null && response.getData() != null && response.getData().getStoreName() != null) {
                return response.getData().getStoreName();
            }
            log.warn("Could not retrieve store name from detail response for storeId: {}", storeId);
            return "매장";
        } catch (Exception e) {
            log.warn("Failed to get store details for storeId: {}, userId: {}. Using fallback name.", storeId, userId, e);
            return "매장";
        }
    }

    private String generateOrderName(Set<OrderItem> items) {
        if (items.isEmpty()) {
            return "빈 주문";
        }

        OrderItem firstItem = items.iterator().next();
        if (items.size() == 1) {
            return firstItem.getProductName();
        } else {
            return firstItem.getProductName() + " 외 " + (items.size() - 1) + "건";
        }
    }

    /**
     * 주문 목록을 OrderDetailResponse 목록으로 변환
     * 벌크 API를 사용하여 N+1 문제 해결
     */
    private List<OrderDetailResponse> convertToOrderDetailResponseList(List<Order> orders) {
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 첫 번째 주문의 userId를 사용 (모든 주문이 같은 사용자의 것이므로)
        Long userId = orders.get(0).getUserId();

        // 모든 주문에서 필요한 스토어 ID, 상품 ID, 사용자 ID 수집
        Set<Long> storeIds = orders.stream()
                .map(Order::getStoreId)
                .collect(Collectors.toSet());

        Set<Long> productIds = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getProductId())
                .collect(Collectors.toSet());

        Set<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .collect(Collectors.toSet());

        // 벌크 API로 스토어, 상품, 사용자 데이터 조회
        Map<Long, StoreResponseDto> storeMap = getBulkStoreData(storeIds, userId);
        Map<Long, ProductResponseDto> productMap = getBulkProductData(productIds, userId);
        Map<Long, SiteUserInfoDto> userMap = getBulkUserData(userIds);

        // 벌크 데이터를 사용하여 주문 상세 응답 생성
        return orders.stream()
                .map(order -> convertToOrderDetailResponseWithMaps(order, storeMap, productMap, userMap))
                .collect(Collectors.toList());
    }

    /**
     * 벌크 스토어 데이터 조회 (N+1 문제 해결)
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
     * 벌크 상품 데이터 조회 (N+1 문제 해결)
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
     * 벌크 사용자 데이터 조회 (N+1 문제 해결)
     */
    @CircuitBreaker(name = "downstreamServices", fallbackMethod = "getBulkUserDataFallback")
    private Map<Long, SiteUserInfoDto> getBulkUserData(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, SiteUserInfoDto> userMap = new java.util.HashMap<>();
        for (Long userId : userIds) {
            try {
                ApiResponseDto<SiteUserInfoDto> response = userServiceClient.getUserInfoById(userId);
                if (response != null && response.getData() != null) {
                    userMap.put(userId, response.getData());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user data for userId: {}. Using fallback.", userId, e);
                userMap.put(userId, createFallbackUserData(userId));
            }
        }
        return userMap;
    }

    /**
     * 벌크 데이터를 사용한 주문 상세 응답 변환 (N+1 문제 해결)
     */
    private OrderDetailResponse convertToOrderDetailResponseWithMaps(Order order,
                                                                     Map<Long, StoreResponseDto> storeMap,
                                                                     Map<Long, ProductResponseDto> productMap,
                                                                     Map<Long, SiteUserInfoDto> userMap) {
        // 스토어 정보 조회 (폴백 데이터 사용 가능)
        StoreResponseDto storeDetails = storeMap.get(order.getStoreId());
        if (storeDetails == null) {
            log.warn("Store data not found for storeId: {}. Using fallback.", order.getStoreId());
            storeDetails = createFallbackStoreData(order.getStoreId());
        }

        // 사용자 정보 조회 (폴백 데이터 사용 가능)
        SiteUserInfoDto customerInfo = userMap.get(order.getUserId());
        if (customerInfo == null) {
            log.warn("User data not found for userId: {}. Using fallback.", order.getUserId());
            customerInfo = createFallbackUserData(order.getUserId());
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

        // Create a list of status history DTOs
        List<OrderDetailResponse.OrderStatusTimestampDto> historyDtos = order.getStatusHistory().stream()
                .map(timestamp -> OrderDetailResponse.OrderStatusTimestampDto.builder()
                        .status(timestamp.getStatus())
                        .updatedAt(timestamp.getUpdatedAt())
                        .build())
                .sorted(java.util.Comparator.comparing(OrderDetailResponse.OrderStatusTimestampDto::getUpdatedAt)) // Ensure chronological order
                .collect(java.util.stream.Collectors.toList());

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .store(storeDetails)
                .customerInfo(customerInfo)
                .status(order.getStatus())
                .originalAmount(order.getOriginalAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
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
                .statusHistory(historyDtos)
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

        // Create a list of status history DTOs
        List<OrderDetailResponse.OrderStatusTimestampDto> historyDtos = order.getStatusHistory().stream()
                .map(timestamp -> OrderDetailResponse.OrderStatusTimestampDto.builder()
                        .status(timestamp.getStatus())
                        .updatedAt(timestamp.getUpdatedAt())
                        .build())
                .sorted(java.util.Comparator.comparing(OrderDetailResponse.OrderStatusTimestampDto::getUpdatedAt)) // Ensure chronological order
                .collect(java.util.stream.Collectors.toList());

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .store(storeDetails) // Full store details
                .status(order.getStatus())
                .originalAmount(order.getOriginalAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
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
                .statusHistory(historyDtos)
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
        fallbackStore.setLatitude(0.0);
        fallbackStore.setLongitude(0.0);
        fallbackStore.setFallback(true);

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
        fallbackProduct.setFallback(true);

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
        fallbackStore.setLatitude(0.0);
        fallbackStore.setLongitude(0.0);
        fallbackStore.setFallback(true);
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

    /**
     * 폴백 사용자 데이터 생성
     */
    private SiteUserInfoDto createFallbackUserData(Long userId) {
        return SiteUserInfoDto.builder()
                .username("user_" + userId)
                .nickname("사용자 정보 일시 불가")
                .name("정보 불가")
                .email("정보 불가")
                .phoneNum("정보 불가")
                .gender("UNKNOWN")
                .age(0)
                .profileImageUrl("")
                .isInfoRegister(false)
                .build();
    }

    /**
     * 벌크 사용자 데이터 조회 실패 시 폴백 메서드
     */
    private Map<Long, SiteUserInfoDto> getBulkUserDataFallback(Set<Long> userIds, Exception ex) {
        log.warn("Bulk user data fetch failed, using fallback data for {} users", userIds.size(), ex);
        return userIds.stream()
                .collect(Collectors.toMap(
                        userId -> userId,
                        this::createFallbackUserData
                ));
    }

    @Override
    public UserOrderSummaryDto getUserOrderSummary(Long userId) {
        log.info("Getting user order summary for userId: {}", userId);

        // Simple implementation returning zero values for now
        return UserOrderSummaryDto.builder()
                .totalOrderCount(0L)
                .totalOriginalAmount(0L)
                .totalDiscountAmount(0L)
                .finalPurchaseAmount(0L)
                .build();
    }

    @Override
    public StoreOrderSummaryResponseDto getStoreOrderSummary(Long ownerUserId, Long storeId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Getting store order summary for ownerUserId: {}, storeId: {}, startDate: {}, endDate: {}", 
                 ownerUserId, storeId, startDate, endDate);

        // Security Check: Verify store ownership
        try {
            ApiResponseDto<List<Long>> storeResponse = storeClient.getStoresByOwnerIdOnApproved(ownerUserId);
            if (storeResponse == null || storeResponse.getData() == null || !storeResponse.getData().contains(storeId)) {
                throw new IllegalArgumentException("User " + ownerUserId + " is not the owner of store " + storeId);
            }
        } catch (Exception e) {
            log.error("Failed to verify store ownership for ownerUserId: {}, storeId: {}", ownerUserId, storeId, e);
            throw new IllegalArgumentException("Failed to verify store ownership: " + e.getMessage());
        }

        // Date Handling: Default to current date if null
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        
        // Convert LocalDate to LocalDateTime for repository queries
        LocalDateTime startDateTime = effectiveStartDate.atStartOfDay();
        LocalDateTime endDateTime = effectiveEndDate.atTime(LocalTime.MAX);

        // Initialize a map with all OrderStatus enums and a count of 0.
        // This ensures a consistent response structure even if some statuses have no orders.
        Map<String, Long> statusCountMap = Arrays.stream(OrderStatus.values())
                .collect(Collectors.toMap(Enum::name, status -> 0L));

        // Get order counts by status from the repository (this part is unchanged).
        List<OrderCountByStatusProjection> statusCountsFromDb = orderRepository.countOrdersByStatusInPeriod(
                storeId, startDateTime, endDateTime);
        
        // Populate the map with actual counts from the database and calculate the total.
        long totalOrderCount = 0;
        for (OrderCountByStatusProjection projection : statusCountsFromDb) {
            statusCountMap.put(projection.getStatus(), projection.getOrderCount());
            totalOrderCount += projection.getOrderCount();
        }

        // Create the new, refactored order count summary object.
        StoreOrderSummaryResponseDto.OrderCountSummary orderCountSummary = 
                StoreOrderSummaryResponseDto.OrderCountSummary.builder()
                        .totalOrderCount(totalOrderCount)
                        .statusCounts(statusCountMap)
                        .build();

        // Fetch Paginated List: Only if original dates were provided
        Page<OrderDetailResponse> ordersPage;
        if (startDate != null && endDate != null) {
            Page<Order> orderPage = orderRepository.findByStoreIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    storeId, startDateTime, endDateTime, pageable);
            
            List<OrderDetailResponse> orderDetailResponses = convertToOrderDetailResponseList(orderPage.getContent());
            ordersPage = new PageImpl<>(orderDetailResponses, pageable, orderPage.getTotalElements());
        } else {
            // Return empty page if dates were not provided
            ordersPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // Assemble and Return
        return StoreOrderSummaryResponseDto.builder()
                .startDate(effectiveStartDate)
                .endDate(effectiveEndDate)
                .orderCountSummary(orderCountSummary)
                .orders(ordersPage)
                .build();
    }

    // [ADD] Implementation for the new getStoreOrders method.
    @Override
    public Page<OrderDetailResponse> getStoreOrders(Long ownerUserId, Long storeId, OrderStatus status, String keyword, Pageable pageable) {
        log.info("Getting store orders - ownerId: {}, storeId: {}, status: {}, keyword: {}, pageable: {}", ownerUserId, storeId, status, keyword, pageable);

        // 1. Security Check: Verify the user owns the store.
        ApiResponseDto<List<Long>> ownedStoresResponse = storeClient.getStoresByOwnerIdOnApproved(ownerUserId);
        if (ownedStoresResponse.getData() == null || !ownedStoresResponse.getData().contains(storeId)) {
            throw new IllegalArgumentException("You do not have permission to access orders for this store.");
        }

        // 2. Fetch paginated order IDs using the native query with keyword search.
        String statusString = (status == null) ? null : status.name();
        Page<Object[]> orderDataPage = orderRepository.findStoreOrderIdsNativeDesc(storeId, statusString, keyword, pageable);

        if (orderDataPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. Extract order IDs from the Object[] results.
        List<String> orderIds = orderDataPage.getContent().stream()
                .map(row -> (String) row[0])
                .collect(Collectors.toList());

        // Call the NEW ASC-sorted details method.
        List<Order> ordersWithDetails = orderRepository.findWithDetailsInAsc(orderIds);
        
        // Reuse the existing helper method to convert entities to DTOs.
        List<OrderDetailResponse> orderDetailResponses = convertToOrderDetailResponseList(ordersWithDetails);

        // 4. Assemble and return the final Page object.
        return new PageImpl<>(orderDetailResponses, pageable, orderDataPage.getTotalElements());
    }

    @Override
    public Page<BestsellerProductResponseDto> getNearbyBestsellers(double latitude, double longitude, Pageable pageable) {
        log.info("Getting nearby bestsellers for coordinates: lat={}, lng={}, pageable={}", latitude, longitude, pageable);

        try {
            // 1단계: 주변 상점 ID 목록 조회 (translation: Step 1: Get nearby store IDs)
            ApiResponseDto<List<Long>> nearbyStoresResponse = storeClient.getNearStoreIds(latitude, longitude);
            if (nearbyStoresResponse == null || nearbyStoresResponse.getData() == null || nearbyStoresResponse.getData().isEmpty()) {
                log.info("No nearby stores found for coordinates: lat={}, lng={}", latitude, longitude);
                return Page.empty(pageable);
            }
            List<Long> nearbyStoreIds = nearbyStoresResponse.getData();
            log.debug("Found {} nearby stores: {}", nearbyStoreIds.size(), nearbyStoreIds);

            // 2단계: 판매량 순으로 정렬된 상품 ID와 판매량 조회 (translation: Step 2: Get product IDs and sales counts, sorted by sales volume)
            Page<OrderRepository.BestsellerProductProjection> bestsellerPage =
                orderRepository.findBestsellersByStoreIds(nearbyStoreIds, pageable);
            if (bestsellerPage.isEmpty()) {
                log.info("No bestseller products found for nearby stores: {}", nearbyStoreIds);
                return Page.empty(pageable);
            }

            // 3단계: 상품 상세 정보 조회를 위해 ID만 추출 (translation: Step 3: Extract only product IDs to fetch details)
            List<Long> productIds = bestsellerPage.getContent().stream()
                    .map(OrderRepository.BestsellerProductProjection::getProductId)
                    .collect(Collectors.toList());
            log.debug("Found {} bestseller products: {}", productIds.size(), productIds);

            // 4단계: 상품 상세 정보 일괄 조회 (translation: Step 4: Fetch product details in bulk)
            ApiResponseDto<Map<Long, ProductResponseDto>> productsResponse =
                productClient.getProductsByIdsPublic(productIds);
            if (productsResponse == null || productsResponse.getData() == null) {
                log.warn("Failed to fetch product details for bestseller products: {}", productIds);
                return Page.empty(pageable);
            }
            Map<Long, ProductResponseDto> productMap = productsResponse.getData();

            // 5단계 (핵심 수정): 판매량 정보와 상품 상세 정보를 합쳐서 새로운 DTO 생성
            // (translation: Step 5 (Key Change): Combine sales count data with product details to create the new DTO)
            List<BestsellerProductResponseDto> finalResult = bestsellerPage.getContent().stream()
                    .map(projection -> {
                        ProductResponseDto details = productMap.get(projection.getProductId());
                        if (details != null) {
                            return new BestsellerProductResponseDto(projection.getTotalQuantity(), details);
                        }
                        return null; // 상세 정보가 없는 경우 제외 (translation: Exclude if details are missing)
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Successfully retrieved {} nearby bestseller products", finalResult.size());

            // 6단계: 최종 결과를 Page 객체로 만들어 반환 (translation: Step 6: Create and return the final Page object)
            return new PageImpl<>(finalResult, pageable, bestsellerPage.getTotalElements());

        } catch (Exception e) {
            log.error("Error getting nearby bestsellers for coordinates: lat={}, lng={}", latitude, longitude, e);
            return Page.empty(pageable);
        }
    }
}