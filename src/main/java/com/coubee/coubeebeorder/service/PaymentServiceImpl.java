package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.remote.dto.PortOnePaymentResponse;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyRequest;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyResponse;
import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.PaymentRepository;
import com.coubee.coubeebeorder.remote.PortOneClient;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentRequest;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.event.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.domain.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final KafkaMessageProducer kafkaMessageProducer;

    @Override
    @Transactional
    public PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request) {
        log.info("Preparing payment for order: {}", orderId);
        
        // Find order by orderId
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + orderId));
        
        // Create Payment entity with READY status
        Payment payment = Payment.createPayment(
                orderId, // Using orderId as paymentId
                order,
                request.getPaymentMethod(),
                order.getTotalAmount()
        );
        
        // Save payment
        Payment savedPayment = paymentRepository.save(payment);
        
        // Set payment to order
        order.setPayment(savedPayment);
        orderRepository.save(order);
        
        // Optional: Call PortOne prepare API for amount verification
        try {
            PortOnePaymentRequest portOneRequest = PortOnePaymentRequest.builder()
                    .merchantUid(orderId)
                    .amount(order.getTotalAmount())
                    .name(generateOrderName(order))
                    .buyerName(order.getRecipientName())
                    .build();
            
            portOneClient.preparePayment(portOneRequest);
            log.info("Payment prepared with PortOne for order: {}", orderId);
        } catch (Exception e) {
            log.warn("Failed to prepare payment with PortOne for order: {}, continuing anyway", orderId, e);
            // Don't fail the preparation if PortOne prepare fails
        }
        
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
            // Call PortOne API to get payment information
            PortOnePaymentResponse response = portOneClient.getPayment(paymentId);
            log.info("Payment status retrieved from PortOne: paymentId={}, status={}", paymentId, response.getStatus());
            return response;
        } catch (Exception e) {
            log.error("Failed to get payment status from PortOne: {}", paymentId, e);
            throw new ApiError("결제 상태 조회에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public boolean handlePaymentWebhook(String paymentId) {
        log.info("Handling payment webhook for: {}", paymentId);
        
        try {
            // Get payment information from PortOne (don't trust webhook payload directly)
            PortOnePaymentResponse portOneResponse = portOneClient.getPayment(paymentId);
            
            if (portOneResponse == null) {
                log.error("Failed to retrieve payment information from PortOne: {}", paymentId);
                return false;
            }
            
            String merchantUid = portOneResponse.getMerchantUid(); // This is our orderId
            
            // Find order and payment by merchant_uid (orderId)
            Order order = orderRepository.findByOrderId(merchantUid)
                    .orElseThrow(() -> new NotFound("주문을 찾을 수 없습니다. Order ID: " + merchantUid));
            
            Payment payment = order.getPayment();
            if (payment == null) {
                log.error("Payment not found for order: {}", merchantUid);
                return false;
            }
            
            // Verify payment amount matches order amount
            if (!portOneResponse.getAmount().equals(order.getTotalAmount())) {
                log.error("Payment amount mismatch for order: {}, expected: {}, actual: {}", 
                        merchantUid, order.getTotalAmount(), portOneResponse.getAmount());
                
                // Cancel the payment due to amount mismatch
                try {
                    // TODO: Implement payment cancellation logic here if needed
                    payment.updateFailedStatus();
                    order.updateStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                    return false;
                } catch (Exception e) {
                    log.error("Failed to cancel mismatched payment: {}", paymentId, e);
                    return false;
                }
            }
            
            // Check if payment is successful
            if ("paid".equals(portOneResponse.getStatus())) {
                // Update payment status to PAID
                payment.updatePaidStatus(
                        portOneResponse.getPgProvider(),
                        portOneResponse.getPgTid(),
                        portOneResponse.getReceiptUrl()
                );
                
                // Update order status to PAID
                order.updateStatus(OrderStatus.PAID);
                orderRepository.save(order);
                
                // Publish stock decrease event to Kafka
                try {
                    OrderEvent stockDecreaseEvent = OrderEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("STOCK_DECREASE")
                            .orderId(merchantUid)
                            .userId(order.getUserId())
                            .storeId(order.getStoreId())
                            .items(order.getItems().stream()
                                    .map(item -> OrderEvent.OrderItemEvent.builder()
                                            .productId(item.getProductId())
                                            .quantity(item.getQuantity())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                    
                    kafkaMessageProducer.publishOrderEvent(stockDecreaseEvent);
                    log.info("Stock decrease event published for paid order: {}", merchantUid);
                } catch (Exception e) {
                    log.error("Failed to publish stock decrease event for order: {}", merchantUid, e);
                    // Don't fail the payment processing if event publishing fails
                }
                
                log.info("Payment webhook processed successfully: orderId={}, paymentId={}", merchantUid, paymentId);
                return true;
            } else {
                log.warn("Payment not successful: paymentId={}, status={}", paymentId, portOneResponse.getStatus());
                
                // Update payment status based on PortOne status
                if ("failed".equals(portOneResponse.getStatus()) || "cancelled".equals(portOneResponse.getStatus())) {
                    payment.updateFailedStatus();
                    order.updateStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                }
                
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error handling payment webhook: {}", paymentId, e);
            return false;
        }
    }

    @Override
    public PortOnePaymentResponse verifyPayment(String paymentId) {
        log.info("Verifying payment: {}", paymentId);
        
        try {
            // Call PortOne API to get payment information for verification
            PortOnePaymentResponse response = portOneClient.getPayment(paymentId);
            log.info("Payment verification completed: paymentId={}, status={}", paymentId, response.getStatus());
            return response;
        } catch (Exception e) {
            log.error("Failed to verify payment: {}", paymentId, e);
            throw new ApiError("결제 검증에 실패했습니다.");
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