package com.coubee.coubeebeorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id")
    private Payment payment;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    private PaymentItem(Payment payment, Long itemId, Integer quantity) {
        this.payment = payment;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public static PaymentItem createPaymentItem(Long itemId, Integer quantity) {
        return PaymentItem.builder()
                .itemId(itemId)
                .quantity(quantity)
                .build();
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}