package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.EventType;
import com.coubee.coubeebeorder.domain.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * V3: 이벤트 타입별 주문 아이템 조회
     * 
     * @param eventType 이벤트 타입
     * @param pageable 페이징 정보
     * @return 해당 이벤트 타입의 주문 아이템 목록
     */
    Page<OrderItem> findByEventTypeOrderByCreatedAtDesc(EventType eventType, Pageable pageable);

    /**
     * V3: 특정 주문의 이벤트 타입별 아이템 조회
     * 
     * @param orderId 주문 ID
     * @param eventType 이벤트 타입
     * @return 해당 주문의 특정 이벤트 타입 아이템 목록
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId AND oi.eventType = :eventType")
    List<OrderItem> findByOrderIdAndEventType(@Param("orderId") String orderId, @Param("eventType") EventType eventType);

    /**
     * V3: 상품별 이벤트 타입 통계 조회
     * 
     * @param productId 상품 ID
     * @param eventType 이벤트 타입
     * @return 해당 상품의 특정 이벤트 타입 아이템 목록
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId = :productId AND oi.eventType = :eventType ORDER BY oi.createdAt DESC")
    List<OrderItem> findByProductIdAndEventTypeOrderByCreatedAtDesc(@Param("productId") Long productId, @Param("eventType") EventType eventType);

    /**
     * V3: 이벤트 타입별 수량 합계 조회
     * 
     * @param productId 상품 ID
     * @param eventType 이벤트 타입
     * @return 해당 상품의 특정 이벤트 타입 총 수량
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId AND oi.eventType = :eventType")
    Integer sumQuantityByProductIdAndEventType(@Param("productId") Long productId, @Param("eventType") EventType eventType);
}
