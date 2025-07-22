package com.coubee.service;

import com.coubee.client.PortOneClient;
import com.coubee.client.ProductServiceClient;
import com.coubee.client.dto.request.PortOnePaymentCancelRequest;
import com.coubee.client.dto.request.ProductIdsRequest;
import com.coubee.client.dto.response.PortOnePaymentCancelResponse;
import com.coubee.client.dto.response.PortOnePaymentResponse;
import com.coubee.client.dto.response.ProductDetailResponse;
import com.coubee.domain.Order;
import com.coubee.domain.OrderItem;
import com.coubee.domain.OrderStatus;
import com.coubee.domain.Payment;
import com.coubee.dto.request.OrderCancelRequest;
import com.coubee.dto.request.OrderCreateRequest;
import com.coubee.dto.request.PaymentReadyRequest;
import com.coubee.dto.response.OrderCreateResponse;
import com.coubee.dto.response.OrderDetailResponse;
import com.coubee.dto.response.OrderListResponse;
import com.coubee.exception.ResourceNotFoundException;
import com.coubee.kafka.producer.OrderKafkaProducer;
import com.coubee.kafka.producer.notification.event.NotificationEvent;
import com.coubee.kafka.producer.stock.event.StockDecreaseEvent;
import com.coubee.kafka.producer.stock.event.StockIncreaseEvent;
import com.coubee.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 주문 서비스 구현체
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final PortOneClient portOneClient;
    private final PaymentService paymentService;
    private final QrCodeService qrCodeService;
    private final OrderKafkaProducer kafkaProducer;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, 
                           ProductServiceClient productServiceClient, 
                           PortOneClient portOneClient,
                           PaymentService paymentService,
                           QrCodeService qrCodeService,
                           @Autowired(required = false) OrderKafkaProducer kafkaProducer) {
        this.orderRepository = orderRepository;
        this.productServiceClient = productServiceClient;
        this.portOneClient = portOneClient;
        this.paymentService = paymentService;
        this.qrCodeService = qrCodeService;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * 주문을 생성하고 결제를 준비합니다.
     *
     * @param userId  사용자 ID
     * @param request 주문 생성 요청 DTO
     * @return 주문 생성 응답 DTO
     */
    @Override
    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        // 1. 상품 정보 조회
        List<Long> productIds = request.getItems().stream()
                .map(OrderCreateRequest.OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        // 테스트용: Product Service 호출 비활성화하고 목업 데이터 사용
        // List<ProductDetailResponse> products = productServiceClient.getProductsByIds(
        //         request.getStoreId(),
        //         new ProductIdsRequest(productIds)
        // );
        
        // 목업 상품 데이터 생성
        List<ProductDetailResponse> products = productIds.stream()
                .map(productId -> ProductDetailResponse.builder()
                        .id(productId)
                        .name("테스트 상품 " + productId)
                        .price(100)
                        .stock(100)
                        .active(true)
                        .build())
                .collect(Collectors.toList());

        // 2. 상품 정보를 Map 형태로 변환 (검색 효율을 위해)
        Map<Long, ProductDetailResponse> productMap = products.stream()
                .collect(Collectors.toMap(ProductDetailResponse::getId, p -> p));

        // 3. 주문 금액 계산 및 재고 확인
        int totalAmount = 0;
        String orderName = "";

        for (OrderCreateRequest.OrderItemRequest item : request.getItems()) {
            ProductDetailResponse product = productMap.get(item.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + item.getProductId());
            }

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock: " + product.getName());
            }

            totalAmount += product.getPrice() * item.getQuantity();
            
            // 주문 이름으로 첫 번째 상품명 사용
            if (orderName.isEmpty()) {
                orderName = product.getName();
            }
        }

        // 주문 상품이 여러 개일 경우 " 외 N건" 형식으로 처리
        if (request.getItems().size() > 1) {
            orderName += " 외 " + (request.getItems().size() - 1) + "건";
        }

        // 4. 주문 ID 생성 (order_ + 전체 UUID)
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "");

        // 5. 주문 엔티티 생성
        Order order = Order.createOrder(orderId, userId, request.getStoreId(), totalAmount, request.getRecipientName());
        
        // 6. QR 코드 토큰 생성 (orderId를 Base64로 인코딩)
        try {
            String qrToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(orderId.getBytes());
            order.setOrderToken(qrToken);
            log.info("QR 토큰 생성 완료: {} -> {}", orderId, qrToken);
        } catch (Exception e) {
            log.warn("QR 토큰 생성 실패: {}", orderId, e);
            // QR 토큰 생성 실패해도 주문 생성은 계속 진행
        }

        // 7. 주문 아이템 엔티티 생성 및 주문에 추가
        for (OrderCreateRequest.OrderItemRequest item : request.getItems()) {
            ProductDetailResponse product = productMap.get(item.getProductId());
            OrderItem orderItem = OrderItem.createOrderItem(
                    order,
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getPrice()
            );
            order.addOrderItem(orderItem);
        }

        // 8. 결제 엔티티 생성
        Payment payment = Payment.createPayment(
                orderId, // 결제 ID로 주문 ID 사용
                order,
                request.getPaymentMethod(), // 원본 결제 수단 그대로 저장
                totalAmount
        );

        // 9. 주문 저장 (Cascade 설정으로 OrderItem, Payment 함께 저장)
        orderRepository.save(order);

        // 10. 결제 서비스를 통한 PortOne 결제 정보 사전 등록 (조건부 호출로 변경)
        
        // 사전 등록을 지원하는 결제 수단 목록 (토스페이, 페이코는 사전 등록 미지원)
        final Set<String> methodsSupportingPrepare = Set.of("CARD", "KAKAOPAY");
        
        String paymentMethod = request.getPaymentMethod();

        // 토스페이, 페이코처럼 사전 등록을 지원하지 않는 결제수단은 이 단계를 건너뜀
        if (methodsSupportingPrepare.contains(paymentMethod.toUpperCase())) {
            try {
                // PaymentReadyRequest는 storeId와 items를 받지만, preparePayment에서는 orderId로 조회하므로 더미 데이터 사용
                List<PaymentReadyRequest.Item> items = request.getItems().stream()
                        .map(item -> new PaymentReadyRequest.Item(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList());
                
                PaymentReadyRequest paymentReadyRequest = new PaymentReadyRequest(request.getStoreId(), items);
                
                paymentService.preparePayment(orderId, paymentReadyRequest);
                log.info("결제 준비 완료 (사전 등록): {}", orderId);
            } catch (Exception e) {
                log.warn("결제 준비(사전 등록) 실패: {} - {}", orderId, e.getMessage());
                // 결제 준비 실패해도 주문은 계속 진행 (결제 시 재시도 가능)
            }
        } else {
            log.info("결제 수단 '{}' 은 사전 등록을 지원하지 않습니다. 사전 등록 단계를 건너뜁니다: {}", paymentMethod, orderId);
        }

        // 11. 재고 감소 이벤트 발행 (Kafka)
        if (kafkaProducer != null) {
            List<StockDecreaseEvent.StockItem> stockItems = request.getItems().stream()
                    .map(item -> StockDecreaseEvent.StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
    
            StockDecreaseEvent stockEvent = StockDecreaseEvent.builder()
                    .orderId(orderId)
                    .items(stockItems)
                    .build();
    
            kafkaProducer.sendStockDecreaseEvent(stockEvent);
        }

        // 12. 응답 생성
        return OrderCreateResponse.builder()
                .orderId(orderId)
                .paymentId(orderId) // 결제 ID로 주문 ID 사용
                .amount(totalAmount)
                .orderName(orderName)
                .buyerName(request.getRecipientName())
                .build();
    }

    /**
     * 주문 ID로 주문 상세 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 상세 정보 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return OrderDetailResponse.from(order);
    }

    /**
     * 사용자 ID로 주문 목록을 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 주문 목록 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderListResponse getUserOrders(Long userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return OrderListResponse.from(orderPage);
    }

    /**
     * 주문을 취소합니다.
     *
     * @param orderId 주문 ID
     * @param request 주문 취소 요청 DTO
     * @return 취소된 주문 상세 정보 DTO
     */
    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request) {
        // 1. 주문 정보 조회
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // 2. 주문 취소 가능 상태인지 확인
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        // 3. Request payment cancellation (only if PAID)
        if (order.getStatus() == OrderStatus.PAID && order.getPayment() != null) {
            PortOnePaymentCancelRequest cancelRequest = PortOnePaymentCancelRequest.builder()
                    .cancelAmount(order.getTotalAmount())
                    .cancelReason(request.getCancelReason())
                    .build();

            PortOnePaymentCancelResponse cancelResponse = portOneClient.cancelPayment(
                    order.getPayment().getPaymentId(),
                    cancelRequest
            );

            // 외부 결제 PG(포트원)의 취소 응답을 확인하여 트랜잭션의 일관성을 보장합니다.
            // cancelResponse가 null이거나, 응답 내용에 따라 실패 여부를 판단해야 합니다.
            // (아래는 예시이며, PortOnePaymentCancelResponse DTO의 실제 구조에 맞게 구현해야 합니다.)
            if (cancelResponse == null) { // 또는 응답 DTO 내부의 상태 코드로 실패를 확인
                log.error("PortOne payment cancellation failed for orderId: {}. Response was null.", orderId);
                // 외부 PG 취소 실패 시, 내부 상태 변경을 막기 위해 예외 발생
                throw new IllegalStateException("Failed to cancel payment with PortOne for orderId: " + orderId);
            }

            // 4. 결제 상태 업데이트
            order.getPayment().cancelPayment(LocalDateTime.now(), request.getCancelReason());
        }

        // 5. 주문 상태 업데이트
        order.updateStatus(OrderStatus.CANCELLED);

        // 6. 재고 증가 이벤트 발행 (Kafka)
        if (kafkaProducer != null) {
            List<StockIncreaseEvent.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockIncreaseEvent.StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
    
            StockIncreaseEvent stockEvent = StockIncreaseEvent.builder()
                    .orderId(orderId)
                    .items(stockItems)
                    .build();
    
            kafkaProducer.sendStockIncreaseEvent(stockEvent);
    
            // 7. 알림 이벤트 발행 (Kafka)
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("orderId", orderId);
            notificationData.put("orderStatus", OrderStatus.CANCELLED.name());
            
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(order.getUserId())
                    .title("Order Cancelled")
                    .body("Your order has been cancelled")
                    .type("ORDER_CANCELLED")
                    .data(notificationData)
                    .build();
    
            kafkaProducer.sendNotificationEvent(notificationEvent);
        }
        
        return OrderDetailResponse.from(order);
    }

} 
