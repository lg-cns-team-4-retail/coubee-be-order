package com.coubee.coubeebeorder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API 응답을 위한 공통 DTO 클래스
 * Jackson 라이브러리의 JSON 직렬화/역직렬화를 지원하기 위해 기본 생성자와 전체 필드 생성자를 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private String code;        // 응답 코드
    private String message;     // 응답 메시지
    private T data;            // 응답 데이터
    private boolean success;   // 성공 여부

    /**
     * 코드와 메시지만으로 응답 객체를 생성하는 생성자
     * @param code 응답 코드
     * @param message 응답 메시지
     */
    private ApiResponseDto(String code, String message) {
        this.code = code;
        this.message = message;
        this.success = "OK".equals(code);
    }

    /**
     * 코드, 메시지, 데이터로 응답 객체를 생성하는 생성자
     * @param code 응답 코드
     * @param message 응답 메시지
     * @param data 응답 데이터
     */
    private ApiResponseDto(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = "OK".equals(code);
    }



    /**
     * 데이터 생성 성공 응답을 생성하는 정적 팩토리 메서드
     * @param data 응답 데이터
     * @return 성공 응답 객체 (success = true)
     */
    public static <T> ApiResponseDto<T> createOk(T data) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message("데이터 생성 요청이 성공하였습니다.")
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 데이터 조회 성공 응답을 생성하는 정적 팩토리 메서드
     * @param data 응답 데이터
     * @return 성공 응답 객체 (success = true)
     */
    public static <T> ApiResponseDto<T> readOk(T data) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message("데이터 조회 요청이 성공하였습니다.")
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 데이터 수정 성공 응답을 생성하는 정적 팩토리 메서드
     * @param data 응답 데이터
     * @return 성공 응답 객체 (success = true)
     */
    public static <T> ApiResponseDto<T> updateOk(T data) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message("데이터 수정 요청이 성공하였습니다.")
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 사용자 정의 메시지로 데이터 수정 성공 응답을 생성하는 정적 팩토리 메서드
     * @param data 응답 데이터
     * @param message 사용자 정의 메시지
     * @return 성공 응답 객체 (success = true)
     */
    public static <T> ApiResponseDto<T> updateOk(T data, String message) {
        return ApiResponseDto.<T>builder()
                .code("OK")
                .message(message)
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 기본 성공 응답을 생성하는 정적 팩토리 메서드 (데이터 없음)
     * @return 성공 응답 객체 (success = true)
     */
    public static ApiResponseDto<String> defaultOk() {
        return ApiResponseDto.createOk(null);
    }

    /**
     * 에러 응답을 생성하는 정적 팩토리 메서드
     * @param code 에러 코드
     * @param message 에러 메시지
     * @return 에러 응답 객체 (success = false)
     */
    public static ApiResponseDto<String> createError(String code, String message) {
        return ApiResponseDto.<String>builder()
                .code(code)
                .message(message)
                .data(null)
                .success(false)
                .build();
    }

    /**
     * 데이터를 포함한 에러 응답을 생성하는 정적 팩토리 메서드
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param data 에러 관련 데이터
     * @return 에러 응답 객체 (success = false)
     */
    public static <T> ApiResponseDto<T> createError(String code, String message, T data) {
        return ApiResponseDto.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .success(false)
                .build();
    }
}