package com.coubee.dto.response;

import com.coubee.domain.Order;
import com.coubee.domain.OrderItem;
import com.coubee.domain.OrderStatus;
import com.coubee.domain.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Detail Response DTO
 */
@Getter
@Builder
public class OrderDetailResponse {

    @Schema(description = "Order ID", example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
    private String orderId;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "Store ID", example = "1")
    private Long storeId;

    @Schema(description = "Order status", example = "PAID")
    private OrderStatus status;

    @Schema(description = "Total order amount", example = "25000")
    private Integer totalAmount;

    @Schema(description = "Recipient name", example = "John Doe")
    private String recipientName;

    @Schema(description = "Order token (QR/barcode)", example = "abcdef123456")
    private String orderToken;

    @Schema(description = "Order creation time", example = "2023-06-01T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Order item list")
    private List<OrderItemResponse> items;

    @Schema(description = "Payment information")
    private PaymentResponse payment;

    /**
     * Order Item Response DTO
     */
    @Getter
    @Builder
    public static class OrderItemResponse {

        @Schema(description = "Product ID", example = "1")
        private Long productId;

        @Schema(description = "Product name", example = "Men's T-shirt")
        private String productName;

        @Schema(description = "Order quantity", example = "2")
        private Integer quantity;

        @Schema(description = "Product price", example = "12500")
        private Integer price;

        @Schema(description = "Total product price", example = "25000")
        private Integer totalPrice;

        /**
         * Create response DTO from order item entity
         */
        public static OrderItemResponse from(OrderItem orderItem) {
            return OrderItemResponse.builder()
                    .productId(orderItem.getProductId())
                    .productName(orderItem.getProductName())
                    .quantity(orderItem.getQuantity())
                    .price(orderItem.getPrice())
                    .totalPrice(orderItem.getTotalPrice())
                    .build();
        }
    }

    /**
     * Payment Response DTO
     */
    @Getter
    @Builder
    public static class PaymentResponse {

        @Schema(description = "Payment ID", example = "payment_01H1J5BFXCZDMG8RP0WCTFSN5Y")
        private String paymentId;

        @Schema(description = "PG provider", example = "kakaopay")
        private String pgProvider;

        @Schema(description = "PG transaction ID", example = "T1234567890")
        private String pgTransactionId;

        @Schema(description = "Payment method", example = "card")
        private String method;

        @Schema(description = "Payment amount", example = "25000")
        private Integer amount;

        @Schema(description = "Payment status", example = "PAID")
        private String status;

        @Schema(description = "Payment completion time", example = "2023-06-01T14:35:00")
        private LocalDateTime paidAt;

        @Schema(description = "Receipt URL", example = "https://portone.io/receipt/...")
        private String receiptUrl;

        /**
         * Create response DTO from payment entity
         */
        public static PaymentResponse from(Payment payment) {
            if (payment == null) {
                return null;
            }
            
            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .pgProvider(payment.getPgProvider())
                    .pgTransactionId(payment.getPgTransactionId())
                    .method(payment.getMethod())
                    .amount(payment.getAmount())
                    .status(payment.getStatus().name())
                    .paidAt(payment.getPaidAt())
                    .receiptUrl(payment.getReceiptUrl())
                    .build();
        }
    }

    /**
     * Create response DTO from order entity
     */
    public static OrderDetailResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .recipientName(order.getRecipientName())
                .orderToken(order.getOrderToken())
                .createdAt(order.getCreatedAt())
                .items(items)
                .payment(PaymentResponse.from(order.getPayment()))
                .build();
    }
} 
