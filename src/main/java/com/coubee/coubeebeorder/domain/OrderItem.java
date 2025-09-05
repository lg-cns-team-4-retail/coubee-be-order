package com.coubee.coubeebeorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    // @Enumerated(EnumType.STRING) // 이 어노테이션은 제거되었습니다
    @Column(name = "event_type")
    private EventType eventType;

    @Column(name = "was_hotdeal", nullable = false)
    private Boolean wasHotdeal = false;

    @Builder
    private OrderItem(Order order, Long productId, String productName, String description, Integer quantity, Integer price, EventType eventType, Boolean wasHotdeal) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
        this.eventType = eventType;
        this.wasHotdeal = wasHotdeal != null ? wasHotdeal : false;
    }

    public static OrderItem createOrderItem(Long productId, String productName, String description, Integer quantity, Integer price) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .description(description)
                .quantity(quantity)
                .price(price)
                .eventType(EventType.PURCHASE) // 기본값으로 PURCHASE 설정
                .wasHotdeal(false)
                .build();
    }

    /**
     * 이벤트 타입을 지정하여 주문 아이템을 생성합니다.
     *
     * @param productId 상품 ID
     * @param productName 상품명
     * @param quantity 수량
     * @param price 가격
     * @param eventType 이벤트 타입
     * @return 생성된 주문 아이템
     */
    public static OrderItem createOrderItemWithEventType(Long productId, String productName, String description, Integer quantity, Integer price, EventType eventType) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .description(description)
                .quantity(quantity)
                .price(price)
                .eventType(eventType)
                .wasHotdeal(false)
                .build();
    }

    /**
     * 핫딜 상태를 포함하여 주문 아이템을 생성합니다
     *
     * @param productId 상품 ID
     * @param productName 상품명
     * @param description 상품 설명
     * @param quantity 수량
     * @param price 가격
     * @param eventType 이벤트 타입
     * @param wasHotdeal 이 아이템이 핫딜 기간 중에 구매되었는지 여부
     * @return 생성된 주문 아이템
     */
    public static OrderItem createOrderItemWithHotdeal(Long productId, String productName, String description, Integer quantity, Integer price, EventType eventType, Boolean wasHotdeal) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .description(description)
                .quantity(quantity)
                .price(price)
                .eventType(eventType)
                .wasHotdeal(wasHotdeal != null ? wasHotdeal : false)
                .build();
    }

    public Integer getTotalPrice() {
        return price * quantity;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 주문 아이템의 이벤트 타입을 업데이트합니다.
     *
     * @param eventType 새로운 이벤트 타입
     */
    public void updateEventType(EventType eventType) {
        this.eventType = eventType;
    }
}