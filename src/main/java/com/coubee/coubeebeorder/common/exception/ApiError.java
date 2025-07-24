package com.coubee.coubeebeorder.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiError {
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    BAD_PARAMETER("BAD_PARAMETER", "잘못된 요청 파라미터입니다."),
    CLIENT_ERROR("CLIENT_ERROR", "클라이언트 요청 오류입니다."),
    SERVER_ERROR("SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    PAYMENT_ERROR("PAYMENT_ERROR", "결제 처리 중 오류가 발생했습니다."),
    ORDER_ERROR("ORDER_ERROR", "주문 처리 중 오류가 발생했습니다.");

    private final String code;
    private final String message;
}