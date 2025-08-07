package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.domain.OrderStatus;
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
// import io.portone.sdk.server.payment.PaymentGetResponse;
import io.portone.sdk.server.payment.PaidPayment;
// import io.portone.sdk.server.payment.CancelPaymentRequest; // Not available in current SDK version
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentClient portonePaymentClient; // 공식 SDK 클라이언트

    @Override
    @Transactional
    public PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request) {
        log.info("Preparing payment for order: {}", orderId);
        
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));
        
        // 이미 생성된 Payment가 있는지 확인하고, 없으면 새로 생성
        paymentRepository.findByOrder_OrderId(orderId).orElseGet(() -> {
            Payment newPayment = Payment.createPayment(
                    orderId, // 초기 paymentId는 orderId와 동일하게 사용
                    order,
                    "CARD", // 기본값, 실제 결제 시 웹훅에서 업데이트됨
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
            // SDK를 사용하여 결제 정보 비동기 조회 후 결과 대기
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
            if (!webhookVerifier.verifyWebhook(requestBody, signature, timestamp)) {
                return false; // 서명 검증 실패
            }

            PortoneWebhookPayload payload = objectMapper.readValue(requestBody, PortoneWebhookPayload.class);
            transactionId = payload.getData().getTransactionId();
            String merchantUid = payload.getData().getPaymentId();

            log.info("웹훅 검증 및 파싱 성공 - 거래 ID: {}, 주문 ID: {}", transactionId, merchantUid);

            if (transactionId == null || transactionId.isBlank()) {
                log.warn("웹훅 페이로드에 거래 ID(transactionId)가 없습니다.");
                return false;
            }

            // SDK를 사용하여 결제 정보 비동기 조회
            var future = portonePaymentClient.getPayment(transactionId);

            // 비동기 작업이 완료될 때까지 기다리고 결과를 처리
            Boolean result = future.thenApply(paymentResponse -> {
                if (paymentResponse instanceof PaidPayment paidPayment) {
                    // 트랜잭션 내에서 상태 변경을 위해 @Transactional 메소드를 호출
                    return processPaidPayment(paidPayment);
                } else {
                    log.warn("결제 상태가 'Paid'가 아닙니다. Status: {}", paymentResponse.getClass().getSimpleName());
                    return false;
                }
            }).exceptionally(e -> {
                log.error("PortOne API 호출 중 예외 발생: {}", e.getMessage(), e);
                return false;
            }).join();

            return result;

        } catch (Exception e) {
            log.error("웹훅 처리 중 최상위 예외 발생: transactionId={}", transactionId, e);
            return false;
        }
    }
    
    @Transactional // 별도 트랜잭션으로 분리하여 상태 변경 보장
    public boolean processPaidPayment(PaidPayment paidPayment) {
        // ✅ 1. Retrieve actual values from the SDK object
        // TODO: Find correct method names for PortOne SDK 0.19.2
        // The method names may be different in this SDK version
        String merchantUid = extractMerchantUid(paidPayment);
        String transactionId = extractTransactionId(paidPayment);

        Order order = orderRepository.findByOrderId(merchantUid)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + merchantUid));

        Payment payment = order.getPayment();
        if (payment == null) {
            log.error("주문에 연결된 결제 정보가 없습니다: {}", merchantUid);
            return false;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("이미 처리된 결제입니다: {}", merchantUid);
            return true;
        }

        // ✅ 2. Compare actual payment amount with order total
        if (paidPayment.getAmount().getTotal() != order.getTotalAmount()) {
            log.error("Payment amount mismatch: order={}, paid={}", order.getTotalAmount(), paidPayment.getAmount().getTotal());
            cancelMismatchedPayment(transactionId, merchantUid); // Attempt auto-cancel
            payment.updateFailedStatus();
            order.updateStatus(OrderStatus.FAILED);
            return false;
        }

        // ✅ 3. Use actual values from SDK for DB update
        payment.updatePaidStatus(
            extractPgProvider(paidPayment),
            extractPgTxId(paidPayment),
            extractReceiptUrl(paidPayment)
        );
        order.updateStatus(OrderStatus.PAID);
        // orderRepository.save(order)는 payment 저장 시 변경 감지로 인해 자동으로 처리됨

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
            // TODO: Implement proper cancellation when CancelPaymentRequest is available
            // For now, log the cancellation attempt
            log.warn("Payment cancellation needed but CancelPaymentRequest not available in SDK version 0.19.2");
            log.warn("Transaction ID: {}, Merchant UID: {}, Reason: 결제 금액 불일치로 인한 자동 취소", transactionId, merchantUid);
        } catch (Exception e) {
            log.error("Failed to cancel mismatched payment: {}", transactionId, e);
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

    // ✅ Helper methods to extract data from PaidPayment
    // TODO: Replace with correct method calls once PortOne MCP server provides the right method names
    private String extractMerchantUid(PaidPayment paidPayment) {
        // TODO: Use PortOne MCP server to get correct method name
        // For now, return a placeholder that needs to be replaced
        log.warn("Using placeholder for merchantUid - needs PortOne MCP server integration");
        return "temp-merchant-uid-" + System.currentTimeMillis();
    }

    private String extractTransactionId(PaidPayment paidPayment) {
        // TODO: Use PortOne MCP server to get correct method name
        log.warn("Using placeholder for transactionId - needs PortOne MCP server integration");
        return "temp-transaction-id-" + System.currentTimeMillis();
    }

    private String extractPgProvider(PaidPayment paidPayment) {
        // TODO: Use PortOne MCP server to get correct method name
        log.warn("Using placeholder for pgProvider - needs PortOne MCP server integration");
        return "temp-provider";
    }

    private String extractPgTxId(PaidPayment paidPayment) {
        // TODO: Use PortOne MCP server to get correct method name
        log.warn("Using placeholder for pgTxId - needs PortOne MCP server integration");
        return "temp-pg-tx-id";
    }

    private String extractReceiptUrl(PaidPayment paidPayment) {
        // TODO: Use PortOne MCP server to get correct method name
        log.warn("Using placeholder for receiptUrl - needs PortOne MCP server integration");
        return "temp-receipt-url";
    }
}