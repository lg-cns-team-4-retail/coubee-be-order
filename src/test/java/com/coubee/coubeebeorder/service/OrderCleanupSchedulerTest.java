package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCleanupScheduler 테스트")
class OrderCleanupSchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderCleanupScheduler orderCleanupScheduler;

    @Test
    @DisplayName("스케줄러가 정상적으로 서비스 메서드를 호출한다")
    void cleanupPendingOrders_ShouldCallOrderService() {
        // Given
        doNothing().when(orderService).cancelStalePendingOrders();

        // When
        orderCleanupScheduler.cleanupPendingOrders();

        // Then
        verify(orderService, times(1)).cancelStalePendingOrders();
    }

    @Test
    @DisplayName("서비스에서 예외가 발생해도 스케줄러는 계속 실행된다")
    void cleanupPendingOrders_ShouldHandleExceptions() {
        // Given
        doThrow(new RuntimeException("Test exception")).when(orderService).cancelStalePendingOrders();

        // When & Then - 예외가 발생해도 메서드가 정상적으로 완료되어야 함
        orderCleanupScheduler.cleanupPendingOrders();

        // Verify that the service method was called despite the exception
        verify(orderService, times(1)).cancelStalePendingOrders();
    }
}
