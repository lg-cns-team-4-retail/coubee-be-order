package com.coubee.coubeebeorder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderTimestamp 엔티티 테스트
 */
class OrderTimestampTest {

    @Test
    @DisplayName("OrderTimestamp 생성 - 현재 시간으로 생성")
    void createTimestamp_WithCurrentTime() {
        // Given
        Order order = Order.createOrder("test_order_123", 1L, 1L, 10000, "Test User");
        OrderStatus status = OrderStatus.PAID;

        // When
        OrderTimestamp timestamp = OrderTimestamp.createTimestamp(order, status);

        // Then
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getOrder()).isEqualTo(order);
        assertThat(timestamp.getStatus()).isEqualTo(status);
        assertThat(timestamp.getUpdatedAt()).isNotNull();
        assertThat(timestamp.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("OrderTimestamp 생성 - 특정 시간으로 생성")
    void createTimestamp_WithSpecificTime() {
        // Given
        Order order = Order.createOrder("test_order_123", 1L, 1L, 10000, "Test User");
        OrderStatus status = OrderStatus.PREPARING;
        LocalDateTime specificTime = LocalDateTime.of(2023, 6, 1, 14, 30, 0);

        // When
        OrderTimestamp timestamp = OrderTimestamp.createTimestamp(order, status, specificTime);

        // Then
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getOrder()).isEqualTo(order);
        assertThat(timestamp.getStatus()).isEqualTo(status);
        assertThat(timestamp.getUpdatedAt()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("OrderTimestamp Builder 패턴 테스트")
    void orderTimestamp_BuilderPattern() {
        // Given
        Order order = Order.createOrder("test_order_456", 2L, 1L, 15000, "Test User 2");
        OrderStatus status = OrderStatus.CANCELLED;
        LocalDateTime updateTime = LocalDateTime.of(2023, 6, 2, 10, 15, 30);

        // When
        OrderTimestamp timestamp = OrderTimestamp.builder()
                .order(order)
                .status(status)
                .updatedAt(updateTime)
                .build();

        // Then
        assertThat(timestamp.getOrder()).isEqualTo(order);
        assertThat(timestamp.getStatus()).isEqualTo(status);
        assertThat(timestamp.getUpdatedAt()).isEqualTo(updateTime);
    }

    @Test
    @DisplayName("OrderTimestamp - null 시간으로 생성 시 현재 시간 자동 설정")
    void createTimestamp_WithNullTime_ShouldSetCurrentTime() {
        // Given
        Order order = Order.createOrder("test_order_789", 3L, 1L, 20000, "Test User 3");
        OrderStatus status = OrderStatus.RECEIVED;

        // When
        OrderTimestamp timestamp = OrderTimestamp.builder()
                .order(order)
                .status(status)
                .updatedAt(null)
                .build();

        // Then
        assertThat(timestamp.getOrder()).isEqualTo(order);
        assertThat(timestamp.getStatus()).isEqualTo(status);
        assertThat(timestamp.getUpdatedAt()).isNotNull();
        assertThat(timestamp.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Order와 OrderTimestamp 관계 테스트")
    void order_OrderTimestamp_Relationship() {
        // Given
        Order order = Order.createOrder("test_order_relationship", 1L, 1L, 10000, "Test User");
        
        // When
        OrderTimestamp timestamp1 = OrderTimestamp.createTimestamp(order, OrderStatus.PENDING);
        OrderTimestamp timestamp2 = OrderTimestamp.createTimestamp(order, OrderStatus.PAID);
        OrderTimestamp timestamp3 = OrderTimestamp.createTimestamp(order, OrderStatus.PREPARING);
        
        order.addStatusHistory(timestamp1);
        order.addStatusHistory(timestamp2);
        order.addStatusHistory(timestamp3);

        // Then
        assertThat(order.getStatusHistory()).hasSize(3);
        assertThat(order.getStatusHistory()).containsExactly(timestamp1, timestamp2, timestamp3);
        
        assertThat(timestamp1.getOrder()).isEqualTo(order);
        assertThat(timestamp2.getOrder()).isEqualTo(order);
        assertThat(timestamp3.getOrder()).isEqualTo(order);
        
        assertThat(timestamp1.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(timestamp2.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(timestamp3.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }
}
