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
import com.coubee.coubeebeorder.remote.PortOneClient;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentCancelRequest;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentResponse;
import com.coubee.coubeebeorder.remote.dto.PortoneWebhookPayload;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final KafkaMessageProducer kafkaMessageProducer;
    
    // ✅✅✅ 웹훅 처리를 위해 아래 의존성들을 추가합니다. ✅✅✅
    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request) {
        log.info("Preparing payment for order: {}", orderId);
        
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));
        
        Payment payment = Payment.createPayment(
                orderId,
                order,
                "card", // 이 부분은 request에서 받아오도록 수정할 수 있습니다.
                order.getTotalAmount()
        );
        
        paymentRepository.save(payment);
        order.setPayment(payment);
        orderRepository.save(order);
        
        // V2 API에서는 사전 등록(prepare) API가 별도로 없으므로 해당 호출은 제거합니다.
        
        log.info("Payment preparation completed for order: {}", orderId);
        
        return PaymentReadyResponse.of(
                order.getRecipientName(),
                generateOrderName(order),
                order.getTotalAmount(),
                orderId
        );
    }

    @Override
    public PortOnePaymentResponse getPaymentStatus(String paymentId) {
        log.info("Getting payment status for: {}", paymentId);
        
        try {
            PortOnePaymentResponse response = portOneClient.getPayment(paymentId);
            log.info("Payment status retrieved from PortOne: paymentId={}, status={}", paymentId, response.getResponse().getStatus());
            return response;
        } catch (Exception e) {
            log.error("Failed to get payment status from PortOne: {}", paymentId, e);
            throw new ApiError("결제 상태 조회에 실패했습니다.");
        }
    }

    // ✅✅✅ 이 메소드가 핵심적인 수정 부분입니다. ✅✅✅
    @Override
    @Transactional
    public boolean handlePaymentWebhook(String signature, String timestamp, String requestBody) {
        log.info("Handling payment webhook in service layer.");
        String transactionId = "unknown";
        String merchantUid = "unknown";

        try {
            // 1. DTO로 파싱
            PortoneWebhookPayload payload = objectMapper.readValue(requestBody, PortoneWebhookPayload.class);
            transactionId = payload.getData().getTransactionId();
            merchantUid = payload.getData().getPaymentId();

            log.info("웹훅 페이로드 파싱 완료 - 거래 ID: {}, 주문 ID: {}", transactionId, merchantUid);

            // 2. 서명 검증
            if (webhookVerifier.isWebhookSecretConfigured()) {
                                if (!webhookVerifier.verifyWebhook(transactionId, signature, timestamp)) {
                    log.warn("웹훅 서명 검증 실패 - 거래 ID: {}", transactionId);
                    return false; 
                }
                log.info("웹훅 서명 검증 성공 - 거래 ID: {}", transactionId);
            }

            // 3. transactionId 유효성 검사
            if (transactionId == null || transactionId.trim().isEmpty()) {
                log.warn("거래 ID(transactionId)가 누락되었습니다.");
                return false;
            }

            // 4. PortOne API를 통해 실제 결제 정보 재조회 및 검증 (기존 로직)
            return processPaymentVerification(transactionId, merchantUid);

        } catch (Exception e) {
            log.error("Error handling payment webhook: transactionId={}, orderId={}", transactionId, merchantUid, e);
            return false;
        }
    }
    
    // handlePaymentWebhook의 실제 처리 로직을 별도 메소드로 분리
    private boolean processPaymentVerification(String transactionId, String merchantUid) {
        PortOnePaymentResponse portOneResponse = portOneClient.getPayment(transactionId);
        
        if (portOneResponse == null || portOneResponse.getResponse() == null) {
            log.error("Failed to retrieve payment information from PortOne: {}", transactionId);
            return false;
        }

        String responseMerchantUid = portOneResponse.getResponse().getMerchant_uid();
        if (!merchantUid.equals(responseMerchantUid)) {
            log.error("Webhook merchant_uid does not match PortOne API response merchant_uid. Webhook: {}, API: {}", merchantUid, responseMerchantUid);
            return false;
        }
        
        Order order = orderRepository.findByOrderId(merchantUid)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + merchantUid));
        
        Payment payment = order.getPayment();
        if (payment == null) {
            log.error("Payment not found for order: {}", merchantUid);
            return false;
        }

        // 이미 처리된 결제건인지 확인 (멱등성)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("Payment already processed for order: {}", merchantUid);
            return true; // 이미 성공한 요청이므로 성공으로 처리
        }
        
        if (!portOneResponse.getResponse().getAmount().equals(order.getTotalAmount())) {
            log.error("Payment amount mismatch for order: {}, expected: {}, actual: {}", 
                    merchantUid, order.getTotalAmount(), portOneResponse.getResponse().getAmount());
            
            // 금액 불일치 시 자동 취소 로직
            cancelMismatchedPayment(transactionId, merchantUid);
            payment.updateFailedStatus();
            order.updateStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            return false;
        }
        
        if ("paid".equals(portOneResponse.getResponse().getStatus())) {
            payment.updatePaidStatus(
                    portOneResponse.getResponse().getPg_provider(),
                    portOneResponse.getResponse().getPg_tid(),
                    portOneResponse.getResponse().getReceipt_url()
            );
            
            order.updateStatus(OrderStatus.PAID);
            orderRepository.save(order);
            
            publishStockDecreaseEvent(order);
            
            log.info("Payment webhook processed successfully: orderId={}, transactionId={}", merchantUid, transactionId);
            return true;
        } else {
            log.warn("Payment not successful: transactionId={}, status={}", transactionId, portOneResponse.getResponse().getStatus());
            if ("failed".equals(portOneResponse.getResponse().getStatus()) || "cancelled".equals(portOneResponse.getResponse().getStatus())) {
                payment.updateFailedStatus();
                order.updateStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            }
            return false;
        }
    }

    @Override
    public PortOnePaymentResponse verifyPayment(String paymentId) {
        log.info("Verifying payment: {}", paymentId);
        
        try {
            return portOneClient.getPayment(paymentId);
        } catch (Exception e) {
            log.error("Failed to verify payment: {}", paymentId, e);
            throw new ApiError("결제 검증에 실패했습니다.");
        }
    }
    
    // 금액 불일치 시 자동 취소 로직
    private void cancelMismatchedPayment(String transactionId, String merchantUid) {
        try {
            PortOnePaymentCancelRequest cancelRequest = PortOnePaymentCancelRequest.builder()
                    .imp_uid(transactionId)
                    .merchant_uid(merchantUid)
                    .reason("결제 금액 불일치로 인한 자동 취소")
                    .build();
            portOneClient.cancelPayment(cancelRequest);
            log.info("Mismatched payment cancelled successfully on PortOne: {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to cancel mismatched payment: {}", transactionId, e);
        }
    }

    // 재고 감소 이벤트 발행 로직
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
            log.info("Stock decrease event published for paid order: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish stock decrease event for order: {}", order.getOrderId(), e);
        }
    }
    
    private String generateOrderName(Order order) {
        if (order.getItems().isEmpty()) {
            return "빈 주문";
        }
        
        OrderItem firstItem = order.getItems().get(0);
        if (order.getItems().size() == 1) {
            return firstItem.getProductName();
        } else {
            return firstItem.getProductName() + " 외 " + (order.getItems().size() - 1) + "건";
        }
    }
}