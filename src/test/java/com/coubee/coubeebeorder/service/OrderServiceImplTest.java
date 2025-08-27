package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.exception.NotFound;
import com.coubee.coubeebeorder.domain.EventType;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.OrderTimestamp;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusResponse;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.OrderTimestampRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTimestampRepository orderTimestampRepository;

    @Mock
    private com.coubee.coubeebeorder.remote.store.StoreClient storeClient;

    @Mock
    private com.coubee.coubeebeorder.remote.product.ProductClient productClient;

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

    @Test
    @DisplayName("사용자 주문 목록 조회 - createdAt 내림차순 정렬 확인")
    void getUserOrders_ShouldReturnSortedResults() throws InterruptedException {
        // Given
        Long userId = 1L;
        PageRequest pageRequest = PageRequest.of(0, 10);

        // order2를 먼저 생성하여 createdAt이 더 빠르도록 설정
        Order order2 = Order.createOrder("order_2", userId, 1L, 20000, "User 1");
        order2.addOrderItem(OrderItem.createOrderItem(2L, "상품 2", 1, 20000));
        Thread.sleep(10); // createdAt 차이를 보장하기 위해 잠시 대기
        Order order1 = Order.createOrder("order_1", userId, 1L, 10000, "User 1");
        order1.addOrderItem(OrderItem.createOrderItem(1L, "상품 1", 2, 5000));

        // findByUserIdOrderByCreatedAtDesc는 최신순(order1, order2)으로 반환
        List<Order> sortedOrders = List.of(order1, order2);
        Page<Order> sortedOrderPage = new PageImpl<>(sortedOrders, pageRequest, 2);

        // findWithDetailsIn은 순서를 보장하지 않으므로, 역순(order2, order1)으로 반환하는 것을 시뮬레이션
        List<Order> unsortedDetails = List.of(order2, order1);

        given(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)).willReturn(sortedOrderPage);
        given(orderRepository.findWithDetailsIn(sortedOrders)).willReturn(unsortedDetails);

        // convertToOrderDetailResponse를 위한 Mock 설정
        given(storeClient.getStoreById(anyLong(), anyLong())).willReturn(com.coubee.coubeebeorder.common.dto.ApiResponseDto.ok(new com.coubee.coubeebeorder.remote.store.StoreResponseDto()));
        given(productClient.getProductById(anyLong(), anyLong())).willReturn(com.coubee.coubeebeorder.common.dto.ApiResponseDto.ok(new com.coubee.coubeebeorder.remote.product.ProductResponseDto()));

        // When
        Page<OrderDetailResponse> result = orderService.getUserOrders(userId, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        // 최종 결과는 최신순(order1, order2)으로 정렬되어야 함
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo("order_1");
        assertThat(result.getContent().get(1).getOrderId()).isEqualTo("order_2");
    }

    @Test
    @DisplayName("주문 상태 업데이트 시 이력 기록 - updateOrderStatusWithHistory")
    void updateOrderStatusWithHistory_Success() {
        // Given
        String orderId = "test_order_123";
        Order order = Order.createOrder(orderId, 1L, 1L, 10000, "Test User");
        order.updateStatus(OrderStatus.PENDING);

        given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // When
        orderService.updateOrderStatusWithHistory(orderId, OrderStatus.PAID);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getStatusHistory()).hasSize(1);

        OrderTimestamp timestamp = order.getStatusHistory().get(0);
        assertThat(timestamp.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(timestamp.getOrder()).isEqualTo(order);
        assertThat(timestamp.getUpdatedAt()).isNotNull();

        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("존재하지 않는 주문 상태 업데이트 시 예외 발생")
    void updateOrderStatusWithHistory_OrderNotFound() {
        // Given
        String orderId = "non_existent_order";
        given(orderRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatusWithHistory(orderId, OrderStatus.PAID))
                .isInstanceOf(NotFound.class)
                .hasMessageContaining("주문을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("주문 상태 변경 시 이력이 누적되는지 확인")
    void orderStatusHistory_Accumulation() {
        // Given
        String orderId = "test_order_history";
        Order order = Order.createOrder(orderId, 1L, 1L, 10000, "Test User");

        given(orderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // When - 여러 번 상태 변경
        orderService.updateOrderStatusWithHistory(orderId, OrderStatus.PAID);
        orderService.updateOrderStatusWithHistory(orderId, OrderStatus.PREPARING);
        orderService.updateOrderStatusWithHistory(orderId, OrderStatus.PREPARED);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARED);
        assertThat(order.getStatusHistory()).hasSize(3);

        assertThat(order.getStatusHistory().get(0).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getStatusHistory().get(1).getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.getStatusHistory().get(2).getStatus()).isEqualTo(OrderStatus.PREPARED);

        // 모든 이력이 같은 주문을 참조하는지 확인
        assertThat(order.getStatusHistory()).allMatch(timestamp -> timestamp.getOrder().equals(order));
    }
}
