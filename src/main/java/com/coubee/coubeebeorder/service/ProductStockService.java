package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.Order;

/**
 * 상품 재고 관리 서비스
 * Product Service와 동기적으로 통신하여 재고를 관리합니다.
 */
public interface ProductStockService {

    /**
     * 주문에 포함된 상품들의 재고를 감소시킵니다.
     * 결제 준비 시점에 호출되어 재고를 선점합니다.
     *
     * @param order 재고를 감소시킬 주문 정보
     * @throws com.coubee.coubeebeorder.common.exception.InsufficientStockException 재고가 부족한 경우
     */
    void decreaseStock(Order order);

    /**
     * 주문에 포함된 상품들의 재고를 증가시킵니다.
     * 주문 취소 시점에 호출되어 재고를 복원합니다.
     *
     * @param order 재고를 증가시킬 주문 정보
     */
    void increaseStock(Order order);
}
