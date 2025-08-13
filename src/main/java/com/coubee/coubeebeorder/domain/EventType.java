package com.coubee.coubeebeorder.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 주문 아이템 이벤트 타입 열거형
 * 
 * 주문 아이템에서 발생할 수 있는 다양한 이벤트 타입을 정의합니다.
 * - PURCHASE: 구매 이벤트 (일반적인 주문 완료)
 * - REFUND: 환불 이벤트 (주문 취소 또는 부분 환불)
 * - EXCHANGE: 교환 이벤트 (상품 교환)
 * - GIFT: 선물 이벤트 (선물하기 기능)
 */
@Getter
@AllArgsConstructor
public enum EventType {
    PURCHASE("Purchase", "구매"),
    REFUND("Refund", "환불"),
    EXCHANGE("Exchange", "교환"),
    GIFT("Gift", "선물");

    private final String englishName;
    private final String koreanName;
}
