package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.remote.user.SiteUserInfoDto;
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
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상점 주문 상세 응답")
public class OrderDetailResponseDto {

    @Schema(description = "주문 ID", example = "order_4aa75a911e78481c8195290208db1fc0")
    private String orderId;

    @Schema(description = "주문 상태", example = "PAID")
    private String status;

    @Schema(description = "총 주문 금액", example = "399700")
    private Integer totalAmount;

    @Schema(description = "수령인 이름", example = "홍길동")
    private String recipientName;

    @Schema(description = "주문 생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    @Schema(description = "상점 정보")
    private StoreInfo store;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemInfo> items;

    @Schema(description = "결제 정보")
    private PaymentInfo payment;

    @Schema(description = "고객 정보")
    private SiteUserInfoDto customerInfo;

    @Schema(description = "주문의 상태 변경 이력")
    private List<OrderStatusTimestampInfo> statusHistory;

    /**
     * 상점 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상점 정보")
    public static class StoreInfo {
        
        @Schema(description = "상점 ID", example = "1037")
        private Long storeId;
        
        @Schema(description = "상점명", example = "장충동 액세서리")
        private String storeName;
    }

    /**
     * 주문 상품 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주문 상품 정보")
    public static class OrderItemInfo {
        
        @Schema(description = "상품명", example = "러블리 베어 에어팟 케이스")
        private String productName;
        
        @Schema(description = "수량", example = "1")
        private Integer quantity;
        
        @Schema(description = "총 가격", example = "28700")
        private Integer totalPrice;
    }

    /**
     * 결제 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결제 정보")
    public static class PaymentInfo {
        
        @Schema(description = "결제 수단", example = "CARD")
        private String method;
        
        @Schema(description = "결제 상태", example = "PAID")
        private String status;
        
        @Schema(description = "결제 완료 시간")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        private LocalDateTime paidAt;
    }

    /**
     * 주문 상태 변경 이력 정보 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "단일 상태 변경 이벤트를 나타냅니다")
    public static class OrderStatusTimestampInfo {
        
        @Schema(description = "상태", example = "PAID")
        private String status;

        @Schema(description = "상태 변경 시간")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        private LocalDateTime updatedAt;
    }
}
