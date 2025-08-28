package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import com.coubee.coubeebeorder.remote.store.StoreResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    @Schema(description = "Order ID", example = "order_b7833686f25b48e0862612345678abcd")
    private String orderId;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "Store ID", example = "1")
    private Long storeId;

    @Schema(description = "Order status", example = "PAID")
    private OrderStatus status;

    @Schema(description = "Original order amount (before discount)", example = "30000")
    private Integer originalAmount;

    @Schema(description = "Discount amount applied", example = "5000")
    private Integer discountAmount;

    @Schema(description = "Total order amount (after discount)", example = "200")
    private Integer totalAmount;

    @Schema(description = "Recipient name", example = "홍길동")
    private String recipientName;



    @Schema(description = "Order creation time", example = "2023-06-01T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Payment completion time (UNIX timestamp)", example = "1672531200")
    private Long paidAtUnix;

    @Schema(description = "Store details")
    private StoreResponseDto store;

    @Schema(description = "Order item list")
    private List<OrderItemResponse> items;

    @Schema(description = "Payment information")
    private PaymentResponse payment;

    @Schema(description = "The complete history of status changes for the order, sorted chronologically.")
    private List<OrderStatusTimestampDto> statusHistory;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Represents a single status change event in an order's history.")
    public static class OrderStatusTimestampDto {

        @Schema(description = "The status of the order at this point in time.", example = "PAID")
        private OrderStatus status;

        @Schema(description = "The timestamp when this status was set.", example = "2023-06-01T14:35:00")
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        @Schema(description = "Full product details")
        private ProductResponseDto product;
        @Schema(description = "Product ID", example = "1")
        private Long productId;

        @Schema(description = "Product name", example = "테스트 상품 1")
        private String productName;

        @Schema(description = "Order quantity", example = "2")
        private Integer quantity;

        @Schema(description = "Product price", example = "100")
        private Integer price;

        @Schema(description = "Total product price", example = "200")
        private Integer totalPrice;

        @Schema(description = "Event type", example = "PURCHASE", allowableValues = {"PURCHASE", "REFUND", "EXCHANGE", "GIFT"})
        private String eventType;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        @Schema(description = "Payment ID", example = "payment_01H1J5BFXCZDMG8RP0WCTFSN5Y")
        private String paymentId;

        @Schema(description = "PG provider", example = "kakaopay")
        private String pgProvider;

        @Schema(description = "Payment method", example = "card")
        private String method;

        @Schema(description = "Payment amount", example = "25000")
        private Integer amount;

        @Schema(description = "Payment status", example = "PAID")
        private String status;

        @Schema(description = "Payment completion time", example = "2023-06-01T14:35:00")
        private LocalDateTime paidAt;
    }
}