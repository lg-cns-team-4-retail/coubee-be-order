package com.coubee.coubeebeorder.common.exception;

/**
 * 재고 부족 예외
 * Product Service에서 재고가 부족할 때 발생하는 예외입니다.
 */
public class InsufficientStockException extends ClientError {
    
    public InsufficientStockException(String message) {
        super(message);
        this.errorCode = "INSUFFICIENT_STOCK";
    }
}
