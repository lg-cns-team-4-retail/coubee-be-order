package com.coubee.coubeebeorder.kafka.producer.product.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 재고 증가 이벤트
 * Product Service로 전송되어 상품 재고를 증가시키는 이벤트 (주문 취소 시)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockIncreaseEvent {

    private String eventId;
    private String orderId;
    private Long userId;
    private LocalDateTime timestamp;
    private List<StockItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockItem {
        private Long productId;
        private Integer quantity;
    }

    /**
     * 재고 증가 이벤트 생성
     */
    public static StockIncreaseEvent create(String orderId, Long userId, List<StockItem> items) {
        return StockIncreaseEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .items(items)
                .build();
    }
}
