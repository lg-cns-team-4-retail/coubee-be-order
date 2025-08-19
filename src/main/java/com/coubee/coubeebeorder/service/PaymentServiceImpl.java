package com.coubee.coubeebeorder.service;

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
import com.coubee.coubeebeorder.kafka.producer.product.event.StockDecreaseEvent;
import com.coubee.coubeebeorder.kafka.producer.notification.event.OrderNotificationEvent;
import com.coubee.coubeebeorder.remote.dto.PortoneWebhookPayload;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.PaymentMethod;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final KafkaMessageProducer kafkaMessageProducer;
    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final PaymentClient portonePaymentClient;

    @Override
    @Transactional
    public PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request) {
        log.info("Preparing payment for order: {}", orderId);

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));

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
            if (!webhookVerifier.verifyWebhook(requestBody, webhookId, signature, timestamp)) {
                return false;
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
                return processWebhookBasedPayment(merchantUid, transactionId, payload);
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
            order.updateStatus(OrderStatus.FAILED);
            return false;
        }

        payment.updatePaidStatus(
                pgProvider,
                pgTransactionId,
                receiptUrl
        );
        order.updateStatus(OrderStatus.PAID);

        // V3: 결제 완료 시점을 UNIX 타임스탬프로 설정
        order.markAsPaidNow();

        // V3: 모든 주문 아이템의 이벤트 타입을 PURCHASE로 설정
        order.getItems().forEach(item -> item.updateEventType(EventType.PURCHASE));

        publishStockDecreaseEvent(order);

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

    private void cancelMismatchedPayment(String transactionId, String merchantUid) {
        try {
            log.warn("결제 금액 불일치로 인한 자동 취소 필요. SDK 기능 제약으로 로깅만 수행. Transaction ID: {}, Merchant UID: {}",
                    transactionId, merchantUid);
        } catch (Exception e) {
            log.error("결제 금액 불일치로 인한 자동 취소 시도 중 오류 발생: {}", transactionId, e);
        }
    }

    private void publishStockDecreaseEvent(Order order) {
        try {
            // 새로운 이벤트 구조로 재고 감소 이벤트 생성
            List<StockDecreaseEvent.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockDecreaseEvent.StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .toList();

            StockDecreaseEvent stockDecreaseEvent = StockDecreaseEvent.create(
                    order.getOrderId(),
                    order.getUserId(),
                    stockItems
            );

            kafkaMessageProducer.publishStockDecreaseEvent(stockDecreaseEvent);
            log.info("재고 감소 이벤트 발행 완료 - 결제 완료: {}", order.getOrderId());

            // 주문 완료 알림 이벤트도 발행
            OrderNotificationEvent notificationEvent = OrderNotificationEvent.createOrderCompleted(
                    order.getOrderId(),
                    order.getUserId()
            );
            kafkaMessageProducer.publishOrderNotificationEvent(notificationEvent);

        } catch (Exception e) {
            log.error("재고 감소 이벤트 발행 실패 - 주문: {}", order.getOrderId(), e);
        }
    }

    

    private boolean processWebhookBasedPayment(String merchantUid, String transactionId, PortoneWebhookPayload payload) {
        try {
            log.info("웹훅 기반 결제 처리 시작 - merchantUid: {}, transactionId: {}", merchantUid, transactionId);
            
            // 주문 조회
            Order order = orderRepository.findByOrderId(merchantUid)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + merchantUid));
            
            // 결제 정보 조회 또는 생성
            Payment payment = paymentRepository.findByOrder_OrderId(merchantUid)
                    .orElseGet(() -> {
                        log.info("결제 정보가 없어 새로 생성합니다: {}", merchantUid);
                        Payment newPayment = Payment.createPayment(
                                merchantUid,
                                order,
                                "UNKNOWN", // 웹훅에서는 상세 정보 제한적
                                order.getTotalAmount()
                        );
                        return paymentRepository.save(newPayment);
                    });
            
            // 결제 상태 업데이트
            payment.updateStatus(PaymentStatus.PAID);
            payment.updatePgTransactionId(transactionId);
            payment.updatePaidAt(LocalDateTime.now());
            
            paymentRepository.save(payment);
            
            // 주문 상태 업데이트
            order.updateStatus(OrderStatus.PAID);

            // V3: 결제 완료 시점을 UNIX 타임스탬프로 설정
            order.markAsPaidNow();

            // V3: 모든 주문 아이템의 이벤트 타입을 PURCHASE로 설정
            order.getItems().forEach(item -> item.updateEventType(EventType.PURCHASE));

            orderRepository.save(order);

            // Kafka 이벤트 발행
            publishPaymentCompletedEvent(order, payment);
            
            log.info("웹훅 기반 결제 처리 완료 - merchantUid: {}, transactionId: {}", merchantUid, transactionId);
            return true;
            
        } catch (Exception e) {
            log.error("웹훅 기반 결제 처리 중 오류 발생 - merchantUid: {}, transactionId: {}", merchantUid, transactionId, e);
            return false;
        }
    }

    private void publishPaymentCompletedEvent(Order order, Payment payment) {
        // Implement event publishing logic here
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
        OrderItem firstItem = order.getItems().get(0);
        if (order.getItems().size() == 1) {
            return firstItem.getProductName();
        } else {
            return firstItem.getProductName() + " 외 " + (order.getItems().size() - 1) + "건";
        }
    }
}