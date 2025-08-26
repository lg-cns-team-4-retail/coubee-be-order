package com.coubee.coubeebeorder.repository;

import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.OrderTimestamp;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.OrderTimestampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderTimestampRepository 테스트
 */
@DataJpaTest
class OrderTimestampRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderTimestampRepository orderTimestampRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder;
    private OrderTimestamp timestamp1;
    private OrderTimestamp timestamp2;
    private OrderTimestamp timestamp3;

    @BeforeEach
    void setUp() {
        // Given - 테스트 주문 생성
        testOrder = Order.createOrder("test_order_123", 1L, 1L, 10000, "Test User");
        entityManager.persistAndFlush(testOrder);

        // 시간 순서대로 상태 변경 이력 생성
        LocalDateTime baseTime = LocalDateTime.of(2023, 6, 1, 10, 0, 0);
        
        timestamp1 = OrderTimestamp.createTimestamp(testOrder, OrderStatus.PENDING, baseTime);
        timestamp2 = OrderTimestamp.createTimestamp(testOrder, OrderStatus.PAID, baseTime.plusMinutes(30));
        timestamp3 = OrderTimestamp.createTimestamp(testOrder, OrderStatus.PREPARING, baseTime.plusHours(1));

        entityManager.persist(timestamp1);
        entityManager.persist(timestamp2);
        entityManager.persist(timestamp3);
        entityManager.flush();
    }

    @Test
    @DisplayName("주문 ID로 상태 이력 조회 - 시간순 정렬")
    void findByOrderIdOrderByUpdatedAtAsc() {
        // When
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc("test_order_123");

        // Then
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(history.get(2).getStatus()).isEqualTo(OrderStatus.PREPARING);
        
        // 시간순 정렬 확인
        assertThat(history.get(0).getUpdatedAt()).isBefore(history.get(1).getUpdatedAt());
        assertThat(history.get(1).getUpdatedAt()).isBefore(history.get(2).getUpdatedAt());
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 조회 시 빈 리스트 반환")
    void findByOrderIdOrderByUpdatedAtAsc_NonExistentOrder() {
        // When
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc("non_existent_order");

        // Then
        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("주문 엔티티 ID로 상태 이력 조회")
    void findByOrderEntityIdOrderByUpdatedAtAsc() {
        // When
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderEntityIdOrderByUpdatedAtAsc(testOrder.getId());

        // Then
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(history.get(2).getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    @DisplayName("여러 주문의 이력이 섞여있을 때 특정 주문만 조회")
    void findByOrderIdOrderByUpdatedAtAsc_MultipleOrders() {
        // Given - 다른 주문 생성
        Order anotherOrder = Order.createOrder("another_order_456", 2L, 1L, 20000, "Another User");
        entityManager.persistAndFlush(anotherOrder);

        OrderTimestamp anotherTimestamp = OrderTimestamp.createTimestamp(anotherOrder, OrderStatus.CANCELLED);
        entityManager.persistAndFlush(anotherTimestamp);

        // When
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc("test_order_123");

        // Then - 원래 주문의 이력만 조회되어야 함
        assertThat(history).hasSize(3);
        assertThat(history).allMatch(timestamp -> timestamp.getOrder().getOrderId().equals("test_order_123"));
    }

    @Test
    @DisplayName("OrderTimestamp 저장 및 조회 테스트")
    void saveAndFind() {
        // Given
        Order newOrder = Order.createOrder("new_order_789", 3L, 1L, 15000, "New User");
        entityManager.persistAndFlush(newOrder);

        OrderTimestamp newTimestamp = OrderTimestamp.createTimestamp(newOrder, OrderStatus.RECEIVED);

        // When
        OrderTimestamp saved = orderTimestampRepository.save(newTimestamp);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrder()).isEqualTo(newOrder);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(saved.getUpdatedAt()).isNotNull();

        // 조회 확인
        List<OrderTimestamp> found = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc("new_order_789");
        assertThat(found).hasSize(1);
        assertThat(found.get(0)).isEqualTo(saved);
    }

    @Test
    @DisplayName("Cascade 삭제 테스트 - 주문 삭제 시 이력도 함께 삭제")
    void cascadeDelete() {
        // Given
        String orderId = testOrder.getOrderId();
        
        // 이력이 존재하는지 확인
        List<OrderTimestamp> historyBeforeDelete = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(orderId);
        assertThat(historyBeforeDelete).hasSize(3);

        // When - 주문 삭제
        orderRepository.delete(testOrder);
        entityManager.flush();

        // Then - 이력도 함께 삭제되어야 함
        List<OrderTimestamp> historyAfterDelete = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(orderId);
        assertThat(historyAfterDelete).isEmpty();
    }
}
