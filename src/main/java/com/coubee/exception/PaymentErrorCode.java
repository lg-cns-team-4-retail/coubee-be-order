package com.coubee.exception;

import com.coubee.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment item information not found."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Product is out of stock."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment information not found."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "Payment amount does not match."),
    NOT_PAID(HttpStatus.BAD_REQUEST, "Payment has not been completed."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
} 
