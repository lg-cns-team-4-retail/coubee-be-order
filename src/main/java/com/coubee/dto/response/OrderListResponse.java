package com.coubee.dto.response;

import com.coubee.domain.Order;
import com.coubee.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order List Response DTO
 */
@Getter
@Builder
public class OrderListResponse {

    @Schema(description = "Order list")
    private List<OrderSummary> orders;

    @Schema(description = "Page information")
    private PageInfo pageInfo;

    /**
     * Order Summary Information DTO
     */
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

        /**
         * Create summary DTO from order entity
         */
        public static OrderSummary from(Order order) {
            String orderName = generateOrderName(order);
            
            return OrderSummary.builder()
                    .orderId(order.getOrderId())
                    .storeId(order.getStoreId())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .createdAt(order.getCreatedAt())
                    .orderName(orderName)
                    .build();
        }

        /**
         * Generate order name from items
         */
        private static String generateOrderName(Order order) {
            if (order.getItems() == null || order.getItems().isEmpty()) {
                return "No order items";
            }

            String firstItemName = order.getItems().get(0).getProductName();
            int totalItems = order.getItems().size();

            if (totalItems == 1) {
                return firstItemName;
            } else {
                return firstItemName + " and " + (totalItems - 1) + " more";
            }
        }
    }

    /**
     * Page Information DTO
     */
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

    /**
     * Create response DTO from order page
     */
    public static OrderListResponse from(Page<Order> orderPage) {
        List<OrderSummary> orders = orderPage.getContent().stream()
                .map(OrderSummary::from)
                .collect(Collectors.toList());

        PageInfo pageInfo = PageInfo.builder()
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();

        return OrderListResponse.builder()
                .orders(orders)
                .pageInfo(pageInfo)
                .build();
    }
} 
