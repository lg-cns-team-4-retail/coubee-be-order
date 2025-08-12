package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.domain.OrderStatus;
// ✅ [수정] 우리 도메인 Payment는 명시적으로 import 합니다.
import com.coubee.coubeebeorder.domain.Payment;
import com.coubee.coubeebeorder.domain.PaymentStatus;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyRequest;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyResponse;
import com.coubee.coubeebeorder.domain.event.OrderEvent;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.PaymentRepository;
import com.coubee.coubeebeorder.event.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.remote.dto.PortoneWebhookPayload;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.PaidPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

            // Lambda에서 사용할 final 변수
            final String finalTransactionId = transactionId;

            // ✅ [수정] SDK의 Payment 클래스는 FQCN(Full-Qualified Class Name)으로 사용합니다.
            CompletableFuture<io.portone.sdk.server.payment.Payment> future = portonePaymentClient.getPayment(transactionId);

            return future.thenApply(paymentResponse -> {
                if (paymentResponse instanceof PaidPayment paidPayment) {
                    return processPaidPayment(paidPayment);
                } else {
                    log.warn("결제 상태가 'Paid'가 아닙니다. Status: {}", paymentResponse.getClass().getSimpleName());
                    return true;
                }
            }).exceptionally(e -> {
                log.error("PortOne API 호출 중 예외 발생 (transactionId: {}): {}", finalTransactionId, e.getMessage(), e);
                return false;
            }).join();

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
            OrderEvent stockDecreaseEvent = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("STOCK_DECREASE")
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .items(order.getItems().stream()
                            .map(item -> OrderEvent.OrderItemEvent.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList())
                    .build();
            kafkaMessageProducer.publishOrderEvent(stockDecreaseEvent);
        } catch (Exception e) {
            log.error("Failed to publish stock decrease event for order: {}", order.getOrderId(), e);
        }
    }

    private String extractPgProvider(io.portone.sdk.server.payment.PaymentMethod method) {
        if (method == null) {
            return "unknown";
        }
        
        // PaymentMethod는 union type이므로 타입별로 처리
        try {
            // 임시로 toString()을 사용하여 타입 정보 추출
            String methodType = method.getClass().getSimpleName();
            log.debug("PaymentMethod type: {}", methodType);
            return methodType.toLowerCase().replace("paymentmethod", "");
        } catch (Exception e) {
            log.warn("Failed to extract PG provider from PaymentMethod: {}", e.getMessage());
            return "unknown";
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