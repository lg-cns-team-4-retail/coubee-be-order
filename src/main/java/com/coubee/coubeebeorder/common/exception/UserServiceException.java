package com.coubee.coubeebeorder.common.exception;

/**
 * User 서비스와의 통신 오류를 명확하게 표현하는 예외 클래스
 * User 서비스 장애 시 권한 문제와 구분하여 처리하기 위해 사용
 */
public class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message);
    }
    
    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
