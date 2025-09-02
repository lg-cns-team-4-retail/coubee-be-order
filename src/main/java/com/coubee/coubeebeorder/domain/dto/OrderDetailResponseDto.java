package com.coubee.coubeebeorder.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상점별 주문 상세 정보 응답 DTO
 * (Store-specific order detail response DTO)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Store order detail response")
public class OrderDetailResponseDto {

    @Schema(description = "Order ID", example = "order_4aa75a911e78481c8195290208db1fc0")
    private String orderId;

    @Schema(description = "Order status", example = "PAID")
    private String status;

    @Schema(description = "Total order amount", example = "399700")
    private Integer totalAmount;

    @Schema(description = "Recipient name", example = "John Doe")
    private String recipientName;

    @Schema(description = "Order creation time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    @Schema(description = "Store information")
    private StoreInfo store;

    @Schema(description = "Order items")
    private List<OrderItemInfo> items;

    @Schema(description = "Payment information")
    private PaymentInfo payment;

    /**
     * 상점 정보 내부 클래스
     * (Store information inner class)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Store information")
    public static class StoreInfo {
        
        @Schema(description = "Store ID", example = "1037")
        private Long storeId;
        
        @Schema(description = "Store name", example = "Jangchungdong Accessory")
        private String storeName;
    }

    /**
     * 주문 상품 정보 내부 클래스
     * (Order item information inner class)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Order item information")
    public static class OrderItemInfo {
        
        @Schema(description = "Product name", example = "Lovely Bear Airpods Case")
        private String productName;
        
        @Schema(description = "Quantity", example = "1")
        private Integer quantity;
        
        @Schema(description = "Total price", example = "28700")
        private Integer totalPrice;
    }

    /**
     * 결제 정보 내부 클래스
     * (Payment information inner class)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Payment information")
    public static class PaymentInfo {
        
        @Schema(description = "Payment method", example = "CARD")
        private String method;
        
        @Schema(description = "Payment status", example = "PAID")
        private String status;
        
        @Schema(description = "Payment completion time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        private LocalDateTime paidAt;
    }
}
