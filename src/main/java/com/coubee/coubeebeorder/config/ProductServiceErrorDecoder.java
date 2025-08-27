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
 * Custom error decoder for handling product service errors
 * Converts Feign exceptions to appropriate domain exceptions
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
                return new NotFound("Product not found");
            case 400:
                // Check if this is an INSUFFICIENT_STOCK error
                String errorCode = extractErrorCodeFromResponse(response);
                if ("INSUFFICIENT_STOCK".equals(errorCode)) {
                    String errorMessage = extractErrorMessageFromResponse(response);
                    return new InsufficientStockException(errorMessage != null ? errorMessage : "재고가 부족합니다.");
                }
                return new ApiError("Invalid product request");
            case 500:
                return new ApiError("Product service internal error");
            case 503:
                return new ApiError("Product service temporarily unavailable");
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
            log.warn("Failed to parse error response body", e);
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
            log.warn("Failed to parse error response body for message", e);
        }
        return null;
    }
}
