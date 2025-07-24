package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderListResponse {

    @Schema(description = "Order list")
    private List<OrderSummary> orders;

    @Schema(description = "Page information")
    private PageInfo pageInfo;

    @Getter
    @Builder
    public static class OrderSummary {
        @Schema(description = "Order ID", example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
        private String orderId;

        @Schema(description = "Store ID", example = "1")
        private Long storeId;

        @Schema(description = "Order status", example = "PAID")
        private OrderStatus status;

        @Schema(description = "Total order amount", example = "25000")
        private Integer totalAmount;

        @Schema(description = "Order creation time", example = "2023-06-01T14:30:00")
        private LocalDateTime createdAt;

        @Schema(description = "Order name (First product + N more)", example = "테스트 상품 1 외 1건")
        private String orderName;
    }

    @Getter
    @Builder
    public static class PageInfo {
        @Schema(description = "Current page", example = "0")
        private int page;

        @Schema(description = "Page size", example = "10")
        private int size;

        @Schema(description = "Total pages", example = "5")
        private int totalPages;

        @Schema(description = "Total elements", example = "42")
        private long totalElements;

        @Schema(description = "Is first page", example = "true")
        private boolean first;

        @Schema(description = "Is last page", example = "false")
        private boolean last;
    }
}