package com.coubee.coubeebeorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Store owner order summary response containing statistics and detailed order list")
public class StoreOrderSummaryResponseDto {

    @Schema(description = "Start date of the summary period", example = "2023-06-01")
    private LocalDate startDate;

    @Schema(description = "End date of the summary period", example = "2023-06-30")
    private LocalDate endDate;

    @Schema(description = "Order count summary statistics")
    private OrderCountSummary orderCountSummary;

    @Schema(description = "Paginated detailed order list")
    private Page<OrderDetailResponse> orders;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Order count statistics grouped by status")
    public static class OrderCountSummary {

        @Schema(description = "Total number of orders", example = "150")
        private Long totalOrderCount;

        @Schema(description = "Number of paid orders (PAID, PREPARING, PREPARED, RECEIVED)", example = "120")
        private Long paidOrderCount;

        @Schema(description = "Number of received orders", example = "100")
        private Long receivedOrderCount;

        @Schema(description = "Number of canceled orders", example = "30")
        private Long canceledOrderCount;
    }
}
