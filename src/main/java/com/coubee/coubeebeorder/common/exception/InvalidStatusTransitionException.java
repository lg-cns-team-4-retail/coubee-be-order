package com.coubee.coubeebeorder.common.exception;

import com.coubee.coubeebeorder.domain.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    
    private final OrderStatus fromStatus;
    private final OrderStatus toStatus;
    
    public InvalidStatusTransitionException(OrderStatus fromStatus, OrderStatus toStatus) {
        super(String.format("Invalid status transition from %s to %s", fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
    
    public OrderStatus getFromStatus() {
        return fromStatus;
    }
    
    public OrderStatus getToStatus() {
        return toStatus;
    }
}
