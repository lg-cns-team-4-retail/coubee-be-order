package com.coubee.coubeebeorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "order_timestamp")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderTimestamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private OrderTimestamp(Order order, OrderStatus status, LocalDateTime updatedAt) {
        this.order = order;
        this.status = status;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /**
     * 현재 시간으로 새로운 OrderTimestamp를 생성합니다
     *
     * @param order 이 타임스탬프가 속한 주문
     * @param status 이 시점의 상태
     * @return 새로운 OrderTimestamp 인스턴스
     */
    public static OrderTimestamp createTimestamp(Order order, OrderStatus status) {
        return OrderTimestamp.builder()
                .order(order)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 특정 시간으로 새로운 OrderTimestamp를 생성합니다
     *
     * @param order 이 타임스탬프가 속한 주문
     * @param status 이 시점의 상태
     * @param updatedAt 특정 타임스탬프
     * @return 새로운 OrderTimestamp 인스턴스
     */
    public static OrderTimestamp createTimestamp(Order order, OrderStatus status, LocalDateTime updatedAt) {
        return OrderTimestamp.builder()
                .order(order)
                .status(status)
                .updatedAt(updatedAt)
                .build();
    }

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
