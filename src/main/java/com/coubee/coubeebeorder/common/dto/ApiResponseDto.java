package com.coubee.coubeebeorder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 응답을 위한 공통 DTO 클래스
 * store-service와 스펙을 일치시키기 위해 success 필드를 제거합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private String code;
    private String message;
    private T data;

    // success 필드를 사용하던 private 생성자 2개는 제거합니다.
    // Lombok의 @AllArgsConstructor가 모든 필드를 받는 생성자를 만들어줍니다.

    public static <T> ApiResponseDto<T> createOk(T data) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message("데이터 생성 요청이 성공하였습니다.")
                .data(data)
                .build(); // .success(true) 제거
    }

    public static <T> ApiResponseDto<T> readOk(T data) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message("데이터 조회 요청이 성공하였습니다.")
                .data(data)
                .build(); // .success(true) 제거
    }

    public static <T> ApiResponseDto<T> updateOk(T data) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message("데이터 수정 요청이 성공하였습니다.")
                .data(data)
                .build(); // .success(true) 제거
    }

    public static <T> ApiResponseDto<T> updateOk(T data, String message) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message(message)
                .data(data)
                .build(); // .success(true) 제거
    }

    public static ApiResponseDto<String> defaultOk() {
        // createOk를 호출하도록 수정합니다.
        return createOk(null);
    }

    public static ApiResponseDto<String> createError(String code, String message) {
        return ApiResponseDto.<String>builder()
                .code(code)
                .message(message)
                .data(null)
                .build(); // .success(false) 제거
    }

    public static <T> ApiResponseDto<T> createError(String code, String message, T data) {
        return ApiResponseDto.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build(); // .success(false) 제거
    }
}