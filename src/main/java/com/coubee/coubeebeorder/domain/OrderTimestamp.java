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
     * Creates a new OrderTimestamp with the current timestamp
     *
     * @param order the order this timestamp belongs to
     * @param status the status at this timestamp
     * @return new OrderTimestamp instance
     */
    public static OrderTimestamp createTimestamp(Order order, OrderStatus status) {
        return OrderTimestamp.builder()
                .order(order)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new OrderTimestamp with a specific timestamp
     *
     * @param order the order this timestamp belongs to
     * @param status the status at this timestamp
     * @param updatedAt the specific timestamp
     * @return new OrderTimestamp instance
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
