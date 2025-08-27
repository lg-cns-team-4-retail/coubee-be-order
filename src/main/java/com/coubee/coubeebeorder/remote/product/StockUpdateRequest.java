package com.coubee.coubeebeorder.remote.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 재고 업데이트 요청 DTO
 * Product Service의 POST /api/product/internal/stock/update 엔드포인트로 전송됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    /**
     * 매장 ID
     */
    private Long storeId;

    /**
     * 재고 업데이트할 상품 목록
     */
    private List<StockItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockItem {
        /**
         * 상품 ID
         */
        private Long productId;

        /**
         * 재고 변경량 (음수: 감소, 양수: 증가)
         */
        private Integer quantityChange;
    }
}
