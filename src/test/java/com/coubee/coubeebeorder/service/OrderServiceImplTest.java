package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.EventType;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusResponse;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("주문 상태 조회 - 성공")
    void getOrderStatus_Success() {
        // Given
        String orderId = "order_test_123";
        Order order = Order.createOrder(orderId, 1L, 1L, 10000, "Test User");
        order.updateStatus(OrderStatus.PAID);

        given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));

        // When
        OrderStatusResponse result = orderService.getOrderStatus(orderId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("V3: 결제 완료된 주문 상세 조회 - paidAtUnix 포함")
    void getOrder_V3_결제완료된주문_paidAtUnix포함() {
        // Given
        String orderId = "order_test_123";
        Order order = Order.createOrder(orderId, 1L, 1L, 10000, "Test User");
        order.updateStatus(OrderStatus.PAID);
        order.markAsPaidNow(); // V3: 결제 완료 시점 설정

        OrderItem orderItem = OrderItem.createOrderItem(1L, "테스트 상품", 2, 5000);
        orderItem.updateEventType(EventType.PURCHASE); // V3: 이벤트 타입 설정
        order.addOrderItem(orderItem);

        given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));

        // When
        OrderDetailResponse result = orderService.getOrder(orderId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getPaidAtUnix()).isNotNull(); // V3: 결제 완료 시점 검증
        assertThat(result.getPaidAtUnix()).isGreaterThan(0L);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getEventType()).isEqualTo("PURCHASE"); // V3: 이벤트 타입 검증
    }

    @Test
    @DisplayName("주문 상태 조회 - 주문이 존재하지 않는 경우")
    void getOrderStatus_OrderNotFound() {
        // Given
        String orderId = "non_existent_order";
        given(orderRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderStatus(orderId))
                .isInstanceOf(NotFound.class)
                .hasMessageContaining("주문을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("주문 상태 조회 - 다양한 상태값 테스트")
    void getOrderStatus_DifferentStatuses() {
        // Test PENDING status
        String pendingOrderId = "order_pending_123";
        Order pendingOrder = Order.createOrder(pendingOrderId, 1L, 1L, 10000, "Test User");
        // Order is created with PENDING status by default

        given(orderRepository.findByOrderId(pendingOrderId)).willReturn(Optional.of(pendingOrder));

        OrderStatusResponse pendingResult = orderService.getOrderStatus(pendingOrderId);
        assertThat(pendingResult.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Test PREPARING status
        String preparingOrderId = "order_preparing_123";
        Order preparingOrder = Order.createOrder(preparingOrderId, 1L, 1L, 10000, "Test User");
        preparingOrder.updateStatus(OrderStatus.PREPARING);

        given(orderRepository.findByOrderId(preparingOrderId)).willReturn(Optional.of(preparingOrder));

        OrderStatusResponse preparingResult = orderService.getOrderStatus(preparingOrderId);
        assertThat(preparingResult.getStatus()).isEqualTo(OrderStatus.PREPARING);

        // Test CANCELLED status
        String cancelledOrderId = "order_cancelled_123";
        Order cancelledOrder = Order.createOrder(cancelledOrderId, 1L, 1L, 10000, "Test User");
        cancelledOrder.updateStatus(OrderStatus.CANCELLED);

        given(orderRepository.findByOrderId(cancelledOrderId)).willReturn(Optional.of(cancelledOrder));

        OrderStatusResponse cancelledResult = orderService.getOrderStatus(cancelledOrderId);
        assertThat(cancelledResult.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Test RECEIVED status
        String receivedOrderId = "order_received_123";
        Order receivedOrder = Order.createOrder(receivedOrderId, 1L, 1L, 10000, "Test User");
        receivedOrder.updateStatus(OrderStatus.RECEIVED);

        given(orderRepository.findByOrderId(receivedOrderId)).willReturn(Optional.of(receivedOrder));

        OrderStatusResponse receivedResult = orderService.getOrderStatus(receivedOrderId);
        assertThat(receivedResult.getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }
}
