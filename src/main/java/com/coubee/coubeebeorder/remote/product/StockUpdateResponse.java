package com.coubee.coubeebeorder.remote.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 재고 업데이트 응답 DTO
 * Product Service의 POST /backend/product/stock/update 엔드포인트에서 반환됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateResponse {

    /**
     * 업데이트 성공 여부
     */
    private Boolean success;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 업데이트된 상품별 재고 정보
     */
    private List<UpdatedStockItem> updatedItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdatedStockItem {
        /**
         * 상품 ID
         */
        private Long productId;

        /**
         * 업데이트 전 재고량
         */
        private Integer previousStock;

        /**
         * 업데이트 후 재고량
         */
        private Integer currentStock;

        /**
         * 재고 변경량
         */
        private Integer quantityChange;
    }
}
