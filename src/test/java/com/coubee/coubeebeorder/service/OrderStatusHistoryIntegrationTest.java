package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.OrderTimestamp;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.OrderTimestampRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 상태 이력 추적 기능 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderStatusHistoryIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTimestampRepository orderTimestampRepository;

    @Test
    @DisplayName("주문 생성부터 완료까지 전체 상태 이력 추적")
    void completeOrderLifecycleHistoryTracking() {
        // Given - 주문 생성 (이 시점에서 PENDING 상태 이력이 기록되어야 함)
        Order order = Order.createOrder("integration_test_order", 1L, 1L, 25000, "Integration Test User");
        
        // 초기 상태 이력 추가 (실제 서비스에서는 createOrder에서 자동으로 추가됨)
        OrderTimestamp initialTimestamp = OrderTimestamp.createTimestamp(order, OrderStatus.PENDING);
        order.addStatusHistory(initialTimestamp);
        
        orderRepository.save(order);

        // When - 상태 변경 시나리오 실행
        // 1. 결제 완료
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PAID);
        
        // 2. 준비 중
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PREPARING);
        
        // 3. 준비 완료
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PREPARED);
        
        // 4. 수령 완료
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.RECEIVED);

        // Then - 이력 검증
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(order.getOrderId());
        
        assertThat(history).hasSize(5); // PENDING + 4번의 상태 변경
        
        // 상태 변경 순서 확인
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(history.get(2).getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(history.get(3).getStatus()).isEqualTo(OrderStatus.PREPARED);
        assertThat(history.get(4).getStatus()).isEqualTo(OrderStatus.RECEIVED);
        
        // 시간 순서 확인
        for (int i = 1; i < history.size(); i++) {
            assertThat(history.get(i).getUpdatedAt())
                    .isAfterOrEqualTo(history.get(i-1).getUpdatedAt());
        }
        
        // 모든 이력이 같은 주문을 참조하는지 확인
        assertThat(history).allMatch(timestamp -> 
                timestamp.getOrder().getOrderId().equals(order.getOrderId()));
        
        // 최종 주문 상태 확인
        Order finalOrder = orderRepository.findByOrderId(order.getOrderId()).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    @DisplayName("주문 취소 시나리오 이력 추적")
    void orderCancellationHistoryTracking() {
        // Given
        Order order = Order.createOrder("cancel_test_order", 2L, 1L, 15000, "Cancel Test User");
        
        // 초기 상태 이력 추가
        OrderTimestamp initialTimestamp = OrderTimestamp.createTimestamp(order, OrderStatus.PENDING);
        order.addStatusHistory(initialTimestamp);
        
        orderRepository.save(order);

        // When - 결제 후 취소 시나리오
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PAID);
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.CANCELLED);

        // Then
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(order.getOrderId());
        
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(history.get(2).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        
        // 최종 상태 확인
        Order finalOrder = orderRepository.findByOrderId(order.getOrderId()).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("결제 실패 시나리오 이력 추적")
    void paymentFailureHistoryTracking() {
        // Given
        Order order = Order.createOrder("failed_test_order", 3L, 1L, 30000, "Failed Test User");
        
        // 초기 상태 이력 추가
        OrderTimestamp initialTimestamp = OrderTimestamp.createTimestamp(order, OrderStatus.PENDING);
        order.addStatusHistory(initialTimestamp);
        
        orderRepository.save(order);

        // When - 결제 실패 시나리오
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.FAILED);

        // Then
        List<OrderTimestamp> history = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(order.getOrderId());
        
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.get(1).getStatus()).isEqualTo(OrderStatus.FAILED);
        
        // 최종 상태 확인
        Order finalOrder = orderRepository.findByOrderId(order.getOrderId()).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("여러 주문의 이력이 독립적으로 관리되는지 확인")
    void multipleOrdersIndependentHistory() {
        // Given - 두 개의 서로 다른 주문 생성
        Order order1 = Order.createOrder("multi_test_order_1", 1L, 1L, 10000, "User 1");
        Order order2 = Order.createOrder("multi_test_order_2", 2L, 1L, 20000, "User 2");
        
        // 초기 상태 이력 추가
        order1.addStatusHistory(OrderTimestamp.createTimestamp(order1, OrderStatus.PENDING));
        order2.addStatusHistory(OrderTimestamp.createTimestamp(order2, OrderStatus.PENDING));
        
        orderRepository.save(order1);
        orderRepository.save(order2);

        // When - 각각 다른 상태 변경
        orderService.updateOrderStatusWithHistory(order1.getOrderId(), OrderStatus.PAID);
        orderService.updateOrderStatusWithHistory(order1.getOrderId(), OrderStatus.PREPARING);
        
        orderService.updateOrderStatusWithHistory(order2.getOrderId(), OrderStatus.CANCELLED);

        // Then - 각 주문의 이력이 독립적으로 관리되는지 확인
        List<OrderTimestamp> history1 = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(order1.getOrderId());
        List<OrderTimestamp> history2 = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(order2.getOrderId());
        
        // Order 1 이력 확인
        assertThat(history1).hasSize(3);
        assertThat(history1.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history1.get(1).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(history1.get(2).getStatus()).isEqualTo(OrderStatus.PREPARING);
        
        // Order 2 이력 확인
        assertThat(history2).hasSize(2);
        assertThat(history2.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history2.get(1).getStatus()).isEqualTo(OrderStatus.CANCELLED);
        
        // 이력이 서로 섞이지 않았는지 확인
        assertThat(history1).allMatch(timestamp -> 
                timestamp.getOrder().getOrderId().equals(order1.getOrderId()));
        assertThat(history2).allMatch(timestamp -> 
                timestamp.getOrder().getOrderId().equals(order2.getOrderId()));
    }

    @Test
    @DisplayName("주문 삭제 시 이력도 함께 삭제되는지 확인 (Cascade)")
    void orderDeletionCascadeHistory() {
        // Given
        Order order = Order.createOrder("delete_test_order", 4L, 1L, 5000, "Delete Test User");
        
        // 여러 상태 이력 추가
        order.addStatusHistory(OrderTimestamp.createTimestamp(order, OrderStatus.PENDING));
        orderRepository.save(order);
        
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PAID);
        orderService.updateOrderStatusWithHistory(order.getOrderId(), OrderStatus.PREPARING);
        
        String orderId = order.getOrderId();
        
        // 이력이 존재하는지 확인
        List<OrderTimestamp> historyBeforeDelete = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(orderId);
        assertThat(historyBeforeDelete).hasSize(3);

        // When - 주문 삭제
        orderRepository.delete(order);

        // Then - 이력도 함께 삭제되었는지 확인
        List<OrderTimestamp> historyAfterDelete = orderTimestampRepository.findByOrderIdOrderByUpdatedAtAsc(orderId);
        assertThat(historyAfterDelete).isEmpty();
    }
}
