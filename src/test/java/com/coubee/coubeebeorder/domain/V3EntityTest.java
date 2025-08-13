package com.coubee.coubeebeorder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V3 데이터베이스 변경사항 엔티티 테스트
 * 
 * paidAtUnix 필드와 eventType 필드의 동작을 검증합니다.
 */
class V3EntityTest {

    @Test
    @DisplayName("Order 엔티티 - paidAtUnix 필드 설정 테스트")
    void order_paidAtUnix_설정_테스트() {
        // Given
        Order 주문 = Order.createOrder("test_order_123", 1L, 1L, 10000, "테스트 고객");
        Long 결제시점 = System.currentTimeMillis() / 1000L;

        // When
        주문.setPaidAtUnix(결제시점);

        // Then
        assertThat(주문.getPaidAtUnix()).isEqualTo(결제시점);
    }

    @Test
    @DisplayName("Order 엔티티 - markAsPaidNow 메서드 테스트")
    void order_markAsPaidNow_메서드_테스트() {
        // Given
        Order 주문 = Order.createOrder("test_order_123", 1L, 1L, 10000, "테스트 고객");
        Long 테스트시작시점 = System.currentTimeMillis() / 1000L;

        // When
        주문.markAsPaidNow();

        // Then
        assertThat(주문.getPaidAtUnix()).isNotNull();
        assertThat(주문.getPaidAtUnix()).isGreaterThanOrEqualTo(테스트시작시점);
        assertThat(주문.getPaidAtUnix()).isLessThanOrEqualTo(System.currentTimeMillis() / 1000L);
    }

    @Test
    @DisplayName("OrderItem 엔티티 - eventType 필드 기본값 테스트")
    void orderItem_eventType_기본값_테스트() {
        // Given & When
        OrderItem 주문아이템 = OrderItem.createOrderItem(1L, "테스트 상품", 2, 1000);

        // Then
        assertThat(주문아이템.getEventType()).isEqualTo(EventType.PURCHASE);
    }

    @Test
    @DisplayName("OrderItem 엔티티 - eventType 지정 생성 테스트")
    void orderItem_eventType_지정생성_테스트() {
        // Given & When
        OrderItem 환불아이템 = OrderItem.createOrderItemWithEventType(1L, "테스트 상품", 1, 1000, EventType.REFUND);
        OrderItem 교환아이템 = OrderItem.createOrderItemWithEventType(2L, "교환 상품", 1, 2000, EventType.EXCHANGE);
        OrderItem 선물아이템 = OrderItem.createOrderItemWithEventType(3L, "선물 상품", 1, 3000, EventType.GIFT);

        // Then
        assertThat(환불아이템.getEventType()).isEqualTo(EventType.REFUND);
        assertThat(교환아이템.getEventType()).isEqualTo(EventType.EXCHANGE);
        assertThat(선물아이템.getEventType()).isEqualTo(EventType.GIFT);
    }

    @Test
    @DisplayName("OrderItem 엔티티 - updateEventType 메서드 테스트")
    void orderItem_updateEventType_메서드_테스트() {
        // Given
        OrderItem 주문아이템 = OrderItem.createOrderItem(1L, "테스트 상품", 2, 1000);
        assertThat(주문아이템.getEventType()).isEqualTo(EventType.PURCHASE);

        // When
        주문아이템.updateEventType(EventType.REFUND);

        // Then
        assertThat(주문아이템.getEventType()).isEqualTo(EventType.REFUND);
    }

    @Test
    @DisplayName("EventType 열거형 - 모든 값 검증 테스트")
    void eventType_모든값_검증_테스트() {
        // Given & When & Then
        assertThat(EventType.PURCHASE.getEnglishName()).isEqualTo("Purchase");
        assertThat(EventType.PURCHASE.getKoreanName()).isEqualTo("구매");

        assertThat(EventType.REFUND.getEnglishName()).isEqualTo("Refund");
        assertThat(EventType.REFUND.getKoreanName()).isEqualTo("환불");

        assertThat(EventType.EXCHANGE.getEnglishName()).isEqualTo("Exchange");
        assertThat(EventType.EXCHANGE.getKoreanName()).isEqualTo("교환");

        assertThat(EventType.GIFT.getEnglishName()).isEqualTo("Gift");
        assertThat(EventType.GIFT.getKoreanName()).isEqualTo("선물");
    }

    @Test
    @DisplayName("Order와 OrderItem 통합 테스트 - V3 필드 포함")
    void order_orderItem_통합_테스트_V3필드포함() {
        // Given
        Order 주문 = Order.createOrder("test_order_123", 1L, 1L, 10000, "테스트 고객");
        OrderItem 구매아이템 = OrderItem.createOrderItem(1L, "테스트 상품 1", 2, 3000);
        OrderItem 선물아이템 = OrderItem.createOrderItemWithEventType(2L, "테스트 상품 2", 1, 4000, EventType.GIFT);

        // When
        주문.addOrderItem(구매아이템);
        주문.addOrderItem(선물아이템);
        주문.markAsPaidNow();

        // Then
        assertThat(주문.getItems()).hasSize(2);
        assertThat(주문.getPaidAtUnix()).isNotNull();
        
        assertThat(구매아이템.getEventType()).isEqualTo(EventType.PURCHASE);
        assertThat(선물아이템.getEventType()).isEqualTo(EventType.GIFT);
        
        assertThat(구매아이템.getOrder()).isEqualTo(주문);
        assertThat(선물아이템.getOrder()).isEqualTo(주문);
    }
}
