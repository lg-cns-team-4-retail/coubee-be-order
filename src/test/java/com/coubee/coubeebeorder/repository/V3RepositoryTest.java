package com.coubee.coubeebeorder.repository;

import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.repository.OrderItemRepository;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V3 리포지토리 테스트
 * 
 * paidAtUnix 및 eventType 필드를 활용한 쿼리 메서드들을 검증합니다.
 */
@DataJpaTest
class V3RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Order 결제완료주문1;
    private Order 결제완료주문2;
    private Order 미결제주문;
    private OrderItem 구매아이템1;
    private OrderItem 구매아이템2;
    private OrderItem 환불아이템;

    @BeforeEach
    void setUp() {
        // 결제 완료된 주문 1
        결제완료주문1 = Order.createOrder("paid_order_1", 1L, 1L, 10000, "고객1");
        결제완료주문1.updateStatus(OrderStatus.PAID);
        결제완료주문1.setPaidAtUnix(1672531200L); // 2023-01-01 00:00:00 UTC
        entityManager.persist(결제완료주문1);

        // 결제 완료된 주문 2
        결제완료주문2 = Order.createOrder("paid_order_2", 2L, 1L, 20000, "고객2");
        결제완료주문2.updateStatus(OrderStatus.PAID);
        결제완료주문2.setPaidAtUnix(1672617600L); // 2023-01-02 00:00:00 UTC
        entityManager.persist(결제완료주문2);

        // 미결제 주문
        미결제주문 = Order.createOrder("unpaid_order", 3L, 1L, 15000, "고객3");
        미결제주문.updateStatus(OrderStatus.PENDING);
        // paidAtUnix는 null로 유지
        entityManager.persist(미결제주문);

        // 구매 아이템 1
        구매아이템1 = OrderItem.createOrderItemWithEventType(1L, "상품1", 2, 5000, EventType.PURCHASE);
        구매아이템1.setOrder(결제완료주문1);
        entityManager.persist(구매아이템1);

        // 구매 아이템 2
        구매아이템2 = OrderItem.createOrderItemWithEventType(2L, "상품2", 1, 20000, EventType.PURCHASE);
        구매아이템2.setOrder(결제완료주문2);
        entityManager.persist(구매아이템2);

        // 환불 아이템
        환불아이템 = OrderItem.createOrderItemWithEventType(1L, "상품1", 1, 5000, EventType.REFUND);
        환불아이템.setOrder(미결제주문);
        entityManager.persist(환불아이템);

        entityManager.flush();
    }

    @Test
    @DisplayName("결제 완료 시점 범위로 주문 조회 테스트")
    void 결제완료시점_범위로_주문조회_테스트() {
        // Given
        Long 시작시점 = 1672531200L; // 2023-01-01 00:00:00 UTC
        Long 종료시점 = 1672617600L; // 2023-01-02 00:00:00 UTC
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Order> 결과 = orderRepository.findByPaidAtUnixBetweenOrderByPaidAtUnixDesc(시작시점, 종료시점, pageable);

        // Then
        assertThat(결과.getContent()).hasSize(2);
        assertThat(결과.getContent().get(0)).isEqualTo(결제완료주문2); // 최신순 정렬
        assertThat(결과.getContent().get(1)).isEqualTo(결제완료주문1);
    }

    @Test
    @DisplayName("결제 완료된 주문만 조회 테스트")
    void 결제완료된_주문만_조회_테스트() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Order> 결과 = orderRepository.findPaidOrdersOrderByPaidAtUnixDesc(pageable);

        // Then
        assertThat(결과.getContent()).hasSize(2);
        assertThat(결과.getContent()).containsExactly(결제완료주문2, 결제완료주문1); // 최신순 정렬
        assertThat(결과.getContent()).doesNotContain(미결제주문);
    }

    @Test
    @DisplayName("사용자별 결제 완료된 주문 조회 테스트")
    void 사용자별_결제완료된_주문조회_테스트() {
        // Given
        Long 사용자ID = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Order> 결과 = orderRepository.findPaidOrdersByUserIdOrderByPaidAtUnixDesc(사용자ID, pageable);

        // Then
        assertThat(결과.getContent()).hasSize(1);
        assertThat(결과.getContent().get(0)).isEqualTo(결제완료주문1);
    }

    @Test
    @DisplayName("이벤트 타입별 주문 아이템 조회 테스트")
    void 이벤트타입별_주문아이템_조회_테스트() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderItem> 구매아이템들 = orderItemRepository.findByEventTypeOrderByCreatedAtDesc(EventType.PURCHASE, pageable);
        Page<OrderItem> 환불아이템들 = orderItemRepository.findByEventTypeOrderByCreatedAtDesc(EventType.REFUND, pageable);

        // Then
        assertThat(구매아이템들.getContent()).hasSize(2);
        assertThat(구매아이템들.getContent()).containsExactlyInAnyOrder(구매아이템1, 구매아이템2);

        assertThat(환불아이템들.getContent()).hasSize(1);
        assertThat(환불아이템들.getContent().get(0)).isEqualTo(환불아이템);
    }

    @Test
    @DisplayName("특정 주문의 이벤트 타입별 아이템 조회 테스트")
    void 특정주문의_이벤트타입별_아이템조회_테스트() {
        // Given
        String 주문ID = "paid_order_1";

        // When
        List<OrderItem> 구매아이템들 = orderItemRepository.findByOrderIdAndEventType(주문ID, EventType.PURCHASE);
        List<OrderItem> 환불아이템들 = orderItemRepository.findByOrderIdAndEventType(주문ID, EventType.REFUND);

        // Then
        assertThat(구매아이템들).hasSize(1);
        assertThat(구매아이템들.get(0)).isEqualTo(구매아이템1);

        assertThat(환불아이템들).isEmpty();
    }

    @Test
    @DisplayName("상품별 이벤트 타입 아이템 조회 테스트")
    void 상품별_이벤트타입_아이템조회_테스트() {
        // Given
        Long 상품ID = 1L;

        // When
        List<OrderItem> 구매아이템들 = orderItemRepository.findByProductIdAndEventTypeOrderByCreatedAtDesc(상품ID, EventType.PURCHASE);
        List<OrderItem> 환불아이템들 = orderItemRepository.findByProductIdAndEventTypeOrderByCreatedAtDesc(상품ID, EventType.REFUND);

        // Then
        assertThat(구매아이템들).hasSize(1);
        assertThat(구매아이템들.get(0)).isEqualTo(구매아이템1);

        assertThat(환불아이템들).hasSize(1);
        assertThat(환불아이템들.get(0)).isEqualTo(환불아이템);
    }

    @Test
    @DisplayName("상품별 이벤트 타입 수량 합계 조회 테스트")
    void 상품별_이벤트타입_수량합계_조회_테스트() {
        // Given
        Long 상품ID = 1L;

        // When
        Integer 구매수량 = orderItemRepository.sumQuantityByProductIdAndEventType(상품ID, EventType.PURCHASE);
        Integer 환불수량 = orderItemRepository.sumQuantityByProductIdAndEventType(상품ID, EventType.REFUND);
        Integer 교환수량 = orderItemRepository.sumQuantityByProductIdAndEventType(상품ID, EventType.EXCHANGE);

        // Then
        assertThat(구매수량).isEqualTo(2); // 구매아이템1의 수량
        assertThat(환불수량).isEqualTo(1); // 환불아이템의 수량
        assertThat(교환수량).isEqualTo(0); // 교환 아이템 없음
    }
}
