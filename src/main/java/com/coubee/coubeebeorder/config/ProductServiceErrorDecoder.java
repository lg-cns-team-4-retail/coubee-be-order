package com.coubee.coubeebeorder.config;

import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.InsufficientStockException;
import com.coubee.coubeebeorder.common.exception.NotFound;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * 상품 서비스 오류 처리를 위한 커스텀 에러 디코더
 * Feign 예외를 적절한 도메인 예외로 변환합니다
 */
@Slf4j
public class ProductServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Product service error: method={}, status={}, reason={}",
                 methodKey, response.status(), response.reason());

        switch (response.status()) {
            case 404:
                return new NotFound("상품을 찾을 수 없습니다");
            case 400:
                // INSUFFICIENT_STOCK 오류인지 확인
                String errorCode = extractErrorCodeFromResponse(response);
                if ("INSUFFICIENT_STOCK".equals(errorCode)) {
                    String errorMessage = extractErrorMessageFromResponse(response);
                    return new InsufficientStockException(errorMessage != null ? errorMessage : "재고가 부족합니다.");
                }
                return new ApiError("잘못된 상품 요청입니다");
            case 500:
                return new ApiError("상품 서비스 내부 오류입니다");
            case 503:
                return new ApiError("상품 서비스가 일시적으로 사용할 수 없습니다");
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }

    /**
     * 응답 본문에서 에러 코드를 추출합니다.
     */
    private String extractErrorCodeFromResponse(Response response) {
        try {
            if (response.body() != null) {
                InputStream inputStream = response.body().asInputStream();
                JsonNode jsonNode = objectMapper.readTree(inputStream);

                // API 응답 구조에 따라 에러 코드 추출
                if (jsonNode.has("errorCode")) {
                    return jsonNode.get("errorCode").asText();
                }
                if (jsonNode.has("code")) {
                    return jsonNode.get("code").asText();
                }
            }
        } catch (IOException e) {
            log.warn("에러 응답 본문 파싱에 실패했습니다", e);
        }
        return null;
    }

    /**
     * 응답 본문에서 에러 메시지를 추출합니다.
     */
    private String extractErrorMessageFromResponse(Response response) {
        try {
            if (response.body() != null) {
                InputStream inputStream = response.body().asInputStream();
                JsonNode jsonNode = objectMapper.readTree(inputStream);

                // API 응답 구조에 따라 에러 메시지 추출
                if (jsonNode.has("message")) {
                    return jsonNode.get("message").asText();
                }
                if (jsonNode.has("errorMessage")) {
                    return jsonNode.get("errorMessage").asText();
                }
            }
        } catch (IOException e) {
            log.warn("메시지를 위한 에러 응답 본문 파싱에 실패했습니다", e);
        }
        return null;
    }
}
