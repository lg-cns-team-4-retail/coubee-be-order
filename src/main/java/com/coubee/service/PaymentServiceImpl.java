package com.coubee.service;

import com.coubee.client.PortOneClient;
import com.coubee.client.dto.request.PortOnePaymentRequest;
import com.coubee.client.dto.response.PortOnePaymentResponse;
import com.coubee.domain.Order;
import com.coubee.domain.OrderStatus;
import com.coubee.domain.Payment;
import com.coubee.domain.PaymentStatus;
import com.coubee.dto.request.PaymentReadyRequest;
import com.coubee.dto.response.PaymentReadyResponse;
import com.coubee.dto.response.PaymentStatusResponse;
import com.coubee.exception.ResourceNotFoundException;
import com.coubee.kafka.producer.OrderKafkaProducer;
import com.coubee.kafka.producer.notification.event.NotificationEvent;
import com.coubee.kafka.producer.stock.event.StockIncreaseEvent;
import com.coubee.repository.OrderRepository;
import com.coubee.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Payment Service Implementation
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;
    private final OrderKafkaProducer kafkaProducer;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, 
                             OrderRepository orderRepository, 
                             PortOneClient portOneClient,
                             @Autowired(required = false) OrderKafkaProducer kafkaProducer) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.portOneClient = portOneClient;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    @Transactional
    public PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request) {
        try {
            log.info("Preparing payment for orderId: {}", orderId);
            
            // 1. 주문 정보 조회
            Order order = orderRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
            
            // 2. 결제 정보 조회 (주문 생성 시 이미 생성됨)
            Payment payment = order.getPayment();
            if (payment == null) {
                throw new IllegalStateException("Payment not found for order: " + orderId);
            }
            
            // 3. 주문명 생성 (첫 번째 상품명 + 외 N건)
            String orderName = "Order " + orderId;
            if (!order.getItems().isEmpty()) {
                orderName = order.getItems().get(0).getProductName();
                if (order.getItems().size() > 1) {
                    orderName += " 외 " + (order.getItems().size() - 1) + "건";
                }
            }
            
            // 4. PortOne에 결제 정보 사전 등록 (실제 환경에서만 호출)
            try {
                PortOnePaymentRequest portOneRequest = PortOnePaymentRequest.builder()
                        .paymentId(payment.getPaymentId())
                        .amount(payment.getAmount())
                        .orderName(orderName)
                        .buyerName(order.getRecipientName())
                        .build();
                
                PortOnePaymentResponse portOneResponse = portOneClient.preparePayment(portOneRequest);
                
                if (portOneResponse != null) {
                    log.info("PortOne payment preparation successful for orderId: {}", orderId);
                } else {
                    log.warn("PortOne payment preparation returned null response for orderId: {}", orderId);
                }
                
            } catch (Exception e) {
                // PortOne API 호출 실패 시 로그만 남기고 계속 진행 (테스트 환경 고려)
                log.warn("PortOne payment preparation failed for orderId: {} - Error: {}", orderId, e.getMessage());
                log.info("Continuing with order creation despite PortOne preparation failure (test mode)");
            }
            
            // 5. 응답 생성 (기존 DTO 구조에 맞춤)
            return PaymentReadyResponse.of(
                    order.getRecipientName(),  // buyerName
                    orderName,                 // name
                    payment.getAmount(),       // amount
                    payment.getPaymentId()     // merchantUid
            );
            
        } catch (Exception e) {
            log.error("Error preparing payment for orderId: {}", orderId, e);
            throw new IllegalStateException("Payment preparation failed: " + e.getMessage());
        }
    }
    
    /**
     * Get payment status
     *
     * @param paymentId Payment ID
     * @return Payment status information
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        
        return PaymentStatusResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .amount(payment.getAmount())
                .receiptUrl(payment.getReceiptUrl())
                .build();
    }

    /**
     * Handle PortOne webhook
     * Update payment status and change order status on successful payment.
     *
     * @param paymentId Payment ID
     * @return Processing result
     */
    @Override
    @Transactional
    public boolean handlePaymentWebhook(String paymentId) {
        try {
            log.info("Processing payment webhook for paymentId: {}", paymentId);
            
            // 1. Get payment info from database
            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
            
            Order order = payment.getOrder();
            if (order == null) {
                log.error("No order linked to payment: {}", paymentId);
                return false;
            }
            
            log.info("Found payment: {} with status: {}", paymentId, payment.getStatus());
            
            // 2. Get payment info from PortOne (실제 결제 상태 확인)
            PortOnePaymentResponse portOnePayment;
            try {
                portOnePayment = verifyPayment(paymentId);
                log.info("PortOne payment status: {}", portOnePayment != null ? portOnePayment.getStatus() : "null");
            } catch (Exception e) {
                log.error("Failed to verify payment from PortOne: {}", paymentId, e);
                return false;
            }
            
            if (portOnePayment == null) {
                log.error("PortOne payment not found: {}", paymentId);
                return false;
            }
            
            // 3. Verify amount
            if (!portOnePayment.getAmount().equals(payment.getAmount())) {
                log.error("Payment amount mismatch expected={}, actual={}", payment.getAmount(), portOnePayment.getAmount());
                failPayment(payment, order, "Payment amount mismatch");
                return false;
            }
            
            // 4. Process based on payment status
            String status = portOnePayment.getStatus();
            
            if ("paid".equalsIgnoreCase(status)) {
                // Payment success
                log.info("Processing successful payment: {}", paymentId);
                completePayment(payment, order, portOnePayment);
                return true;
            } else if ("failed".equalsIgnoreCase(status)) {
                // Payment failed
                log.warn("Processing failed payment: {}", paymentId);
                failPayment(payment, order, portOnePayment.getFailReason());
                return false;
            } else if ("cancelled".equalsIgnoreCase(status)) {
                // Payment cancelled
                log.warn("Processing cancelled payment: {}", paymentId);
                cancelPayment(payment, order, "Cancelled by PortOne");
                return false;
            } else {
                // Other status
                log.warn("Unhandled payment status: {} for paymentId: {}", status, paymentId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error processing payment webhook for paymentId: {}", paymentId, e);
            return false;
        }
    }

    /**
     * Verify payment information from PortOne
     *
     * @param paymentId Payment ID
     * @return PortOne payment response
     */
    @Override
    public PortOnePaymentResponse verifyPayment(String paymentId) {
        return portOneClient.getPayment(paymentId);
    }

    /**
     * Process successful payment
     */
    private void completePayment(Payment payment, Order order, PortOnePaymentResponse portOnePayment) {
        // Update payment info
        payment.completePayment(
                portOnePayment.getPgProvider(),
                portOnePayment.getPgTid(),
                portOnePayment.getPaidAt(),
                portOnePayment.getReceiptUrl()
        );
        
        // Generate and save orderQR (orderId encoded to Base64)
        try {
            String orderQR = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(order.getOrderId().getBytes("UTF-8"));
            order.setOrderQR(orderQR);
            log.info("OrderQR generated for orderId: {} -> {}", order.getOrderId(), orderQR);
        } catch (Exception e) {
            log.error("Failed to generate orderQR for orderId: {}", order.getOrderId(), e);
        }
        
        // Order status update (handled automatically in Payment entity)
        
        // Send notification event
        if (kafkaProducer != null) {
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("orderId", order.getOrderId());
            notificationData.put("orderStatus", OrderStatus.PAID.name());
            
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(order.getUserId())
                    .title("Payment Completed")
                    .body("Your order payment has been completed: " + order.getOrderId())
                    .type("PAYMENT_COMPLETED")
                    .data(notificationData)
                    .build();
            
            kafkaProducer.sendNotificationEvent(notificationEvent);
        }
    }

    /**
     * Process failed payment
     */
    private void failPayment(Payment payment, Order order, String failReason) {
        // Update payment info
        payment.failPayment(LocalDateTime.now(), failReason);
        
        // Order status update (handled automatically in Payment entity)
        
        // Send stock increase event
        if (kafkaProducer != null) {
            List<StockIncreaseEvent.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockIncreaseEvent.StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            StockIncreaseEvent stockEvent = StockIncreaseEvent.builder()
                    .orderId(order.getOrderId())
                    .items(stockItems)
                    .build();
            
            kafkaProducer.sendStockIncreaseEvent(stockEvent);
            
            // Send notification event
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("orderId", order.getOrderId());
            notificationData.put("orderStatus", OrderStatus.FAILED.name());
            notificationData.put("failReason", failReason);
            
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(order.getUserId())
                    .title("Payment Failed")
                    .body("Your order payment has failed: " + failReason)
                    .type("PAYMENT_FAILED")
                    .data(notificationData)
                    .build();
            
            kafkaProducer.sendNotificationEvent(notificationEvent);
        }
    }

    /**
     * Process cancelled payment
     */
    private void cancelPayment(Payment payment, Order order, String cancelReason) {
        // Update payment info
        payment.cancelPayment(LocalDateTime.now(), cancelReason);
        
        // Order status update (handled automatically in Payment entity)
        
        // Send stock increase event
        if (kafkaProducer != null) {
            List<StockIncreaseEvent.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockIncreaseEvent.StockItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            StockIncreaseEvent stockEvent = StockIncreaseEvent.builder()
                    .orderId(order.getOrderId())
                    .items(stockItems)
                    .build();
            
            kafkaProducer.sendStockIncreaseEvent(stockEvent);
            
            // Send notification event
            Map<String, String> notificationData = new HashMap<>();
            notificationData.put("orderId", order.getOrderId());
            notificationData.put("orderStatus", OrderStatus.CANCELLED.name());
            
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(order.getUserId())
                    .title("Payment Cancelled")
                    .body("Your order payment has been cancelled")
                    .type("PAYMENT_CANCELLED")
                    .data(notificationData)
                    .build();
            
            kafkaProducer.sendNotificationEvent(notificationEvent);
        }
    }
} 
