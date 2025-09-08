package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl - cancelStalePendingOrders 테스트")
class OrderServiceImplCleanupTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductStockService productStockService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("오래된 PENDING 주문이 없으면 아무것도 처리하지 않는다")
    void cancelStalePendingOrders_NoStaleOrders_ShouldDoNothing() {
        // Given
        when(orderRepository.findStalePendingOrders(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        orderService.cancelStalePendingOrders();

        // Then
        ArgumentCaptor<LocalDateTime> cutoffTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findStalePendingOrders(cutoffTimeCaptor.capture());
        
        // Verify cutoff time is approximately 15 minutes ago
        LocalDateTime cutoffTime = cutoffTimeCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusMinutes(15);
        assertThat(cutoffTime).isBetween(expectedCutoff.minusSeconds(5), expectedCutoff.plusSeconds(5));
        
        // Verify no stock restoration was called
        verify(productStockService, never()).increaseStock(any(Order.class));
    }

    @Test
    @DisplayName("오래된 PENDING 주문들을 FAILED로 변경하고 재고를 복원한다")
    void cancelStalePendingOrders_WithStaleOrders_ShouldCancelAndRestoreStock() {
        // Given
        Order staleOrder1 = mock(Order.class);
        Order staleOrder2 = mock(Order.class);
        when(staleOrder1.getOrderId()).thenReturn("ORDER-001");
        when(staleOrder2.getOrderId()).thenReturn("ORDER-002");
        
        List<Order> staleOrders = Arrays.asList(staleOrder1, staleOrder2);
        when(orderRepository.findStalePendingOrders(any(LocalDateTime.class)))
                .thenReturn(staleOrders);

        // When
        orderService.cancelStalePendingOrders();

        // Then
        // Verify repository was called with correct cutoff time
        ArgumentCaptor<LocalDateTime> cutoffTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findStalePendingOrders(cutoffTimeCaptor.capture());
        
        LocalDateTime cutoffTime = cutoffTimeCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusMinutes(15);
        assertThat(cutoffTime).isBetween(expectedCutoff.minusSeconds(5), expectedCutoff.plusSeconds(5));

        // Verify stock restoration was called for each order
        verify(productStockService).increaseStock(staleOrder1);
        verify(productStockService).increaseStock(staleOrder2);
        
        // Verify orders were processed
        verify(staleOrder1).updateStatus(OrderStatus.FAILED);
        verify(staleOrder2).updateStatus(OrderStatus.FAILED);
    }
}
