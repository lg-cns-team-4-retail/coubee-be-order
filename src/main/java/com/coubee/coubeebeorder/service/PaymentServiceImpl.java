package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.EventType;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.domain.OrderStatus;
// ✅ [수정] 우리 도메인 Payment는 명시적으로 import 합니다.
import com.coubee.coubeebeorder.domain.Payment;
import com.coubee.coubeebeorder.domain.PaymentStatus;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyRequest;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyResponse;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.PaymentRepository;
import com.coubee.coubeebeorder.kafka.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.kafka.producer.notification.event.OrderNotificationEvent;
import com.coubee.coubeebeorder.remote.dto.PortoneWebhookPayload;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import com.coubee.coubeebeorder.remote.store.StoreResponseDto;
import com.coubee.coubeebeorder.remote.product.ProductClient;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
import com.coubee.coubeebeorder.domain.ProcessedWebhook;
import com.coubee.coubeebeorder.domain.repository.ProcessedWebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.PaymentMethod;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy; // ★★★ Lazy import 추가 ★★★

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;

@Slf4j // ✅ [추가] @Slf4j 어노테이션 추가
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final OrderService orderService;
    private final ProductStockService productStockService;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final PaymentClient portonePaymentClient;
    private final StoreClient storeClient;
    private final ProductClient productClient;

    private PaymentServiceImpl self; // ★★★ (translation: Add a field for self-injection)

    @Autowired
    public void setSelf(@Lazy PaymentServiceImpl self) { // ★★★ 파라미터에 @Lazy 어노테이션 추가 ★★★
        this.self = self;
    }

    @Override
    @Transactional
    public PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request) {
        log.info("Preparing payment for order: {}", orderId);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

        // 재고 감소 처리 - 결제 준비 시점에 재고를 선점합니다.
        // InsufficientStockException이 발생하면 결제 준비 과정이 중단됩니다.
        log.info("재고 감소 처리 시작 - 주문 ID: {}", orderId);
        productStockService.decreaseStock(order);
        log.info("재고 감소 처리 완료 - 주문 ID: {}", orderId);

        try {
            // 재고 감소 후 로컬 DB 작업들을 try-catch로 감싸서 실패 시 보상 트랜잭션 실행
            paymentRepository.findByOrder_OrderId(orderId).orElseGet(() -> {
                Payment newPayment = Payment.createPayment(
                        orderId,
                        order,
                        "CARD",
                        order.getTotalAmount()
                );
                order.setPayment(newPayment);
                return paymentRepository.save(newPayment);
            });

            log.info("Payment preparation completed for order: {}", orderId);

            return PaymentReadyResponse.of(
                    order.getRecipientName(),
                    generateOrderName(order),
                    order.getTotalAmount(),
                    orderId
            );
        } catch (Exception e) {
            // 로컬 DB 작업 실패 시 보상 트랜잭션 실행: 재고 복원
            log.error("Payment preparation failed after stock decrement for order: {}. Executing compensating transaction.", orderId, e);

            try {
                productStockService.increaseStock(order);
                log.info("Compensating transaction completed: stock restored for order: {}", orderId);
            } catch (Exception compensationException) {
                log.error("CRITICAL: Compensating transaction failed for order: {}. Manual intervention required.", orderId, compensationException);
            }

            // 원본 예외를 ApiError로 래핑하여 트랜잭션 롤백 및 클라이언트 에러 응답 보장
            throw new ApiError("결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public Object getPaymentStatus(String paymentId) {
        log.info("Getting payment status for: {}", paymentId);
        try {
            return portonePaymentClient.getPayment(paymentId).join();
        } catch (Exception e) {
            log.error("Failed to get payment status from PortOne: {}", paymentId, e);
            throw new ApiError("결제 상태 조회에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public boolean handlePaymentWebhook(String webhookId, String signature, String timestamp, String requestBody) {
        String transactionId = "unknown";
        try {
            // 1. 웹훅 서명 검증
            if (!webhookVerifier.verifyWebhook(requestBody, webhookId, signature, timestamp)) {
                return false;
            }

            // 2. 웹훅 ID 기반 멱등성 체크 - 데이터베이스에 저장 시도
            if (webhookId != null && !webhookId.isBlank()) {
                try {
                    ProcessedWebhook processedWebhook = ProcessedWebhook.create(webhookId);
                    processedWebhookRepository.save(processedWebhook);
                    log.info("웹훅 ID 저장 성공 - 새로운 웹훅 처리 시작: {}", webhookId);
                } catch (DataIntegrityViolationException e) {
                    log.warn("이미 처리된 웹훅입니다. 멱등성 보장으로 성공 응답 반환: webhookId={}", webhookId);
                    return true; // 이미 처리된 웹훅이므로 성공으로 응답
                }
            } else {
                log.warn("웹훅 ID가 없습니다. 멱등성 체크를 건너뜁니다.");
            }

            PortoneWebhookPayload payload = objectMapper.readValue(requestBody, PortoneWebhookPayload.class);
            transactionId = payload.getData().getTransactionId();
            String merchantUid = payload.getData().getPaymentId();

            log.info("웹훅 검증 및 파싱 성공 - 거래 ID: {}, 주문 ID: {}", transactionId, merchantUid);

            if (transactionId == null || transactionId.isBlank()) {
                log.warn("웹훅 페이로드에 거래 ID(transactionId)가 없습니다.");
                return false;
            }

            // 임시 해결책: S2S 검증 건너뛰고 웹훅 이벤트만 믿고 처리
            // TODO: 테스트 모드 API 키 적용 후 S2S 검증 로직 복원 필요
            log.info("임시 해결책: S2S 검증 건너뛰고 웹훅 이벤트 기반으로 결제 상태 업데이트");

            // 웹훅 이벤트 타입에 따라 처리
            String eventType = payload.getType();
            if ("Transaction.Paid".equals(eventType)) {
                log.info("Transaction.Paid 이벤트 처리 - 결제 완료 상태로 업데이트");
                
                // 1. Perform all DB updates first within this primary transaction.
                Order order = orderRepository.findByOrderId(merchantUid)
                        .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다: " + merchantUid)); // (translation: Order not found:)
                
                Payment payment = paymentRepository.findByOrder_OrderId(merchantUid).orElseGet(() -> {
                    log.info("결제 정보가 없어 새로 생성합니다: {}", merchantUid); // (translation: Creating new payment info as none exists:)
                    Payment newPayment = Payment.createPayment(
                            merchantUid,
                            order,
                            "UNKNOWN", // 웹훅에서는 상세 정보 제한적 (translation: Limited detail info from webhook)
                            order.getTotalAmount()
                    );
                    return paymentRepository.save(newPayment);
                });
                
                orderService.updateOrderStatusWithHistory(merchantUid, OrderStatus.PAID);
                order.markAsPaidNow();
                order.getItems().forEach(item -> item.updateEventType(EventType.PURCHASE));
                payment.updateStatus(PaymentStatus.PAID);
                payment.updatePgTransactionId(transactionId);
                payment.updatePaidAt(LocalDateTime.now());
                
                log.info("주문 및 결제 상태 PAID로 업데이트 완료: {}", merchantUid); // (translation: Order and payment status updated to PAID:)
                
                // 2. After DB logic is complete, call the notification method via the self-proxy.
                self.notifyOwner(order.getOrderId(), order.getStoreId(), order.getUserId());
                
                return true;
            } else if ("Transaction.Ready".equals(eventType)) {
                log.info("Transaction.Ready 이벤트 처리 - 결제 준비 상태");
                return true; // Ready 상태는 별도 처리 불필요
            } else {
                log.warn("처리되지 않은 웹훅 이벤트 타입: {}", eventType);
                return true;
            }

        } catch (Exception e) {
            log.error("웹훅 처리 중 최상위 예외 발생: transactionId={}", transactionId, e);
            return false;
        }
    }

    @Transactional
    public boolean processPaidPayment(PaidPayment paidPayment) {
        // PortOne V2에서 id는 paymentId (merchant_uid)에 해당
        String merchantUid = paidPayment.getId();
        String transactionId = paidPayment.getTransactionId();
        long paidAmount = paidPayment.getAmount().getTotal();
        
        // PaymentMethod에서 provider 정보 추출 (타입별 처리)
        String pgProvider = extractPgProvider(paidPayment.getMethod());
        
        String pgTransactionId = paidPayment.getPgTxId() != null ? paidPayment.getPgTxId() : "";
        String receiptUrl = paidPayment.getReceiptUrl() != null ? paidPayment.getReceiptUrl() : "";

        Order order = orderRepository.findByOrderId(merchantUid)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + merchantUid));

        Payment payment = order.getPayment();
        if (payment == null) {
            log.error("주문에 연결된 결제 정보가 없습니다: {}", merchantUid);
            return false;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("이미 'PAID' 상태로 처리된 주문입니다. (멱등성): {}", merchantUid);
            return true;
        }

        if (paidAmount != order.getTotalAmount()) {
            log.error("결제 금액 불일치! 주문 금액: {}, 실제 결제 금액: {}. 자동 취소를 시도합니다.", order.getTotalAmount(), paidAmount);
            cancelMismatchedPayment(transactionId, merchantUid);
            payment.updateFailedStatus();
            orderService.updateOrderStatusWithHistory(merchantUid, OrderStatus.FAILED);
            return false;
        }

        payment.updatePaidStatus(
                pgProvider,
                pgTransactionId,
                receiptUrl
        );
        orderService.updateOrderStatusWithHistory(merchantUid, OrderStatus.PAID);

        // V3: 결제 완료 시점을 UNIX 타임스탬프로 설정
        order.markAsPaidNow();

        // V3: 모든 주문 아이템의 이벤트 타입을 PURCHASE로 설정
        order.getItems().forEach(item -> item.updateEventType(EventType.PURCHASE));

        // 재고 감소는 이미 결제 준비 시점에 처리되었으므로 여기서는 제거
        log.info("결제 성공 처리 완료: 주문 ID {}", merchantUid);
        return true;
    }

    @Override
    public Object verifyPayment(String paymentId) {
        log.info("Verifying payment with SDK: {}", paymentId);
        try {
            return portonePaymentClient.getPayment(paymentId).join();
        } catch (Exception e) {
            log.error("Failed to verify payment: {}", paymentId, e);
            throw new ApiError("결제 검증에 실패했습니다.");
        }
    }

    /**
     * Implements the logic for creating and completing a test order.
     */
    @Override
    @Transactional
    public String createAndCompleteTestOrder(com.coubee.coubeebeorder.domain.dto.TestOrderCreateRequest request) {
        log.info("Creating and force-completing test order for request: {}", request);

        // 1. Product Service에서 실제 상품 정보 조회 (가격을 가져오기 위함)
        // (Fetch actual product information from Product Service (to get the price))
        com.coubee.coubeebeorder.common.dto.ApiResponseDto<com.coubee.coubeebeorder.remote.product.ProductResponseDto> productResponse = productClient.getProductById(request.getProductId(), request.getUserId());
        if (productResponse == null || productResponse.getData() == null) {
            throw new com.coubee.coubeebeorder.common.exception.NotFound("Test failed: Product not found with ID: " + request.getProductId());
        }
        com.coubee.coubeebeorder.remote.product.ProductResponseDto product = productResponse.getData();

        // 2. 리팩토링된 금액 계산 로직을 적용하여 원가, 할인가, 최종 결제액을 계산합니다.
        // (Applies the refactored amount calculation logic to determine original amount, discount, and final payment amount.)
        int totalOriginAmount = product.getOriginPrice() * request.getQuantity();
        int totalSaleAmount = product.getSalePrice() * request.getQuantity();
        int productDiscountAmount = totalOriginAmount - totalSaleAmount;
        
        // 테스트 주문에서는 핫딜 할인이 없다고 가정합니다.
        // (For test orders, assume there is no hotdeal discount.)
        int hotdealDiscountAmount = 0;
        
        // 총 할인액은 상품 할인액과 핫딜 할인액의 합계입니다.
        // (Total discount amount is the sum of product discount and hotdeal discount.)
        int totalDiscountAmount = productDiscountAmount + hotdealDiscountAmount;
        
        // 최종 결제 금액은 판매가에서 핫딜 할인액을 뺀 값입니다.
        // (Final payment amount is the sale amount minus hotdeal discount.)
        int finalPaymentAmount = totalSaleAmount - hotdealDiscountAmount;

        // 3. 조회한 상품 정보로 주문 데이터 생성
        // (Create order data with the fetched product information)
        String orderId = "test_order_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String recipientName = "Test User";
        
        // store-service에서 storeName을 가져옵니다.
        // (translation: Fetch storeName from the store-service.)
        com.coubee.coubeebeorder.common.dto.ApiResponseDto<com.coubee.coubeebeorder.remote.store.StoreResponseDto> storeResponse = storeClient.getStoreById(request.getStoreId(), request.getUserId());
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new com.coubee.coubeebeorder.common.exception.NotFound("Test failed: Store not found with ID: " + request.getStoreId());
        }
        String storeName = storeResponse.getData().getStoreName();
        
        Order order = Order.createOrder(orderId, request.getUserId(), request.getStoreId(), storeName, totalOriginAmount, totalDiscountAmount, finalPaymentAmount, recipientName);
        OrderItem item = OrderItem.createOrderItem(product.getProductId(), product.getProductName(), product.getDescription(), request.getQuantity(), product.getSalePrice());
        order.addOrderItem(item);

        // 4. 결제 데이터 생성
        // (Create payment data)
        Payment payment = Payment.createPayment(orderId, order, "CARD", finalPaymentAmount);
        order.setPayment(payment);

        // 5. 주문 및 결제 상태를 강제로 'PAID'로 변경
        // (Forcefully update order and payment status to 'PAID')
        order.updateStatus(OrderStatus.PAID);
        order.markAsPaidNow();
        order.getItems().forEach(orderItem -> orderItem.updateEventType(EventType.PURCHASE));

        payment.updateStatus(PaymentStatus.PAID);
        payment.updatePaidStatus("test-pg", "test-pg-tid-" + orderId, "http://test.receipt.url");

        // 6. [FIX] 데이터베이스에 먼저 저장합니다.
        // ([FIX] First, save to the database.)
        // Cascade 설정으로 인해 Order, OrderItem, Payment가 모두 저장됩니다.
        // (Due to Cascade settings, Order, OrderItem, and Payment will all be saved.)
        orderRepository.save(order);
        log.info("Test order saved to database with orderId: {}", orderId);

        // 7. [FIX] 저장된 후에 상태 변경 이력을 기록합니다.
        // ([FIX] Record the status change history after saving.)
        // 이렇게 함으로써 updateOrderStatusWithHistory가 DB에서 주문을 조회할 시점에는 이미 주문 데이터가 존재하게 되어 NotFound 예외가 발생하지 않습니다.
        // (This ensures the order data already exists when updateOrderStatusWithHistory queries the DB, preventing a NotFound exception.)
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PAID);

        // 8. After DB logic, call the notification method via the self-proxy.
        self.notifyOwner(order.getOrderId(), order.getStoreId(), order.getUserId());

        return orderId;
    }

    // ★★★ (translation: Add this new method for notifications) ★★★
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOwner(String orderId, Long storeId, Long userId) {
        log.info("새로운 트랜잭션에서 점주 알림을 시작합니다. Order ID: {}", orderId); // (translation: Starting owner notification in a new transaction. Order ID: {})
        try {
            // This method focuses only on external calls, using data passed as arguments.
            ApiResponseDto<StoreResponseDto> storeResponse = storeClient.getStoreById(storeId, userId);
            String storeName = storeResponse.getData() != null ? storeResponse.getData().getStoreName() : "매장"; // (translation: "Store")
            
            ApiResponseDto<Long> ownerIdResponse = storeClient.getOwnerIdByStoreId(storeId);
            
            if (ownerIdResponse != null && ownerIdResponse.isSuccess() && ownerIdResponse.getData() != null) {
                Long ownerId = ownerIdResponse.getData();
                OrderNotificationEvent forOwner = OrderNotificationEvent.createNewOrderNotificationForOwner(
                        orderId, ownerId, storeName);
                kafkaMessageProducer.publishOrderNotificationEvent(forOwner);
                log.info("점주에게 신규 주문 알림 발행 성공. Order: {}, Owner ID: {}", orderId, ownerId); // (translation: Successfully published new order notification to owner.)
            } else {
                log.error("점주 ID 조회 실패. 알림 미발송. StoreId: {}", storeId); // (translation: Failed to get owner ID. Notification not sent.)
            }
        } catch (Exception e) {
            // If an exception occurs here, the main order transaction is already safely committed.
            // We can log this error for monitoring or add to a retry queue.
            log.error("점주 알림 발행 중 최종 예외 발생. Order: {}", orderId, e); // (translation: Final exception occurred while publishing owner notification.)
        }
    }

    private void cancelMismatchedPayment(String transactionId, String merchantUid) {
        try {
            log.warn("결제 금액 불일치로 인한 자동 취소 필요. SDK 기능 제약으로 로깅만 수행. Transaction ID: {}, Merchant UID: {}",
                    transactionId, merchantUid);
        } catch (Exception e) {
            log.error("결제 금액 불일치로 인한 자동 취소 시도 중 오류 발생: {}", transactionId, e);
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



    private String extractPgProvider(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "UNKNOWN";
        }
        
        // PaymentMethod는 union type이므로 런타임 클래스명으로 판단
        String className = paymentMethod.getClass().getSimpleName();
        log.debug("PaymentMethod 클래스: {}", className);
        
        // 클래스명 기반으로 PG 제공자 추출
        if (className.contains("Card")) {
            return "CARD";
        } else if (className.contains("VirtualAccount")) {
            return "VIRTUAL_ACCOUNT";
        } else if (className.contains("Transfer")) {
            return "TRANSFER";
        } else if (className.contains("Mobile")) {
            return "MOBILE";
        } else if (className.contains("EasyPay")) {
            return "EASY_PAY";
        } else {
            return "UNKNOWN";
        }
    }

    private String generateOrderName(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "주문 상품 없음";
        }
        OrderItem firstItem = order.getItems().iterator().next();
        if (order.getItems().size() == 1) {
            return firstItem.getProductName();
        } else {
            return firstItem.getProductName() + " 외 " + (order.getItems().size() - 1) + "건";
        }
    }
}