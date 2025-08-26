package com.coubee.coubeebeorder.remote.store;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.config.FeignConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StoreClient 통합 테스트
 * WireMock을 사용하여 Store 서비스의 응답을 모킹합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8080)
@ActiveProfiles("test")
class StoreClientTest {

    @Autowired
    private StoreClient storeClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("스토어 조회 성공 - 정상적인 응답")
    void getStoreById_Success() throws Exception {
        // Given
        Long storeId = 1L;
        Long userId = 100L;

        StoreResponseDto storeResponse = new StoreResponseDto();
        storeResponse.setStoreId(storeId);
        storeResponse.setStoreName("테스트 스토어");
        storeResponse.setDescription("테스트 스토어 설명");
        storeResponse.setStoreImg("https://example.com/store.jpg");
        storeResponse.setAddress("서울시 강남구 테스트로 123");
        storeResponse.setPhoneNumber("02-1234-5678");
        storeResponse.setBusinessHours("09:00-22:00");
        storeResponse.setCategory("카페");
        storeResponse.setOwnerId(50L);

        ApiResponseDto<StoreResponseDto> apiResponse = ApiResponseDto.readOk(storeResponse);

        stubFor(get(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(apiResponse))));

        // When
        ApiResponseDto<StoreResponseDto> result = storeClient.getStoreById(storeId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getStoreId()).isEqualTo(storeId);
        assertThat(result.getData().getStoreName()).isEqualTo("테스트 스토어");
        assertThat(result.getData().getDescription()).isEqualTo("테스트 스토어 설명");
        assertThat(result.getData().getAddress()).isEqualTo("서울시 강남구 테스트로 123");
        assertThat(result.getData().getPhoneNumber()).isEqualTo("02-1234-5678");
        assertThat(result.getData().getBusinessHours()).isEqualTo("09:00-22:00");
        assertThat(result.getData().getCategory()).isEqualTo("카페");
        assertThat(result.getData().getOwnerId()).isEqualTo(50L);

        // Verify the request was made correctly
        verify(getRequestedFor(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString())));
    }

    @Test
    @DisplayName("스토어 조회 실패 - 404 Not Found")
    void getStoreById_NotFound() {
        // Given
        Long storeId = 999L;
        Long userId = 100L;

        stubFor(get(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString()))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"NOT_FOUND\",\"message\":\"Store not found\",\"success\":false}")));

        // When & Then
        assertThatThrownBy(() -> storeClient.getStoreById(storeId, userId))
                .isInstanceOf(FeignException.class);

        // Verify the request was made correctly
        verify(getRequestedFor(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString())));
    }

    @Test
    @DisplayName("스토어 조회 실패 - 500 Internal Server Error")
    void getStoreById_InternalServerError() {
        // Given
        Long storeId = 1L;
        Long userId = 100L;

        stubFor(get(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString()))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"INTERNAL_ERROR\",\"message\":\"Internal server error\",\"success\":false}")));

        // When & Then
        assertThatThrownBy(() -> storeClient.getStoreById(storeId, userId))
                .isInstanceOf(FeignException.class);

        // Verify the request was made correctly
        verify(getRequestedFor(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString())));
    }

    @Test
    @DisplayName("스토어 조회 실패 - 잘못된 응답 형식")
    void getStoreById_InvalidResponse() {
        // Given
        Long storeId = 1L;
        Long userId = 100L;

        stubFor(get(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json")));

        // When & Then
        assertThatThrownBy(() -> storeClient.getStoreById(storeId, userId))
                .isInstanceOf(FeignException.class);
    }

    @Test
    @DisplayName("스토어 조회 - 필수 헤더 누락 시 요청 확인")
    void getStoreById_WithoutAuthHeader() {
        // Given
        Long storeId = 1L;
        Long userId = 100L;

        // Mock successful response
        StoreResponseDto storeResponse = new StoreResponseDto();
        storeResponse.setStoreId(storeId);
        storeResponse.setStoreName("테스트 스토어");

        ApiResponseDto<StoreResponseDto> apiResponse = ApiResponseDto.readOk(storeResponse);

        stubFor(get(urlEqualTo("/api/store/detail/" + storeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"OK\",\"message\":\"Success\",\"data\":{\"storeId\":1,\"storeName\":\"테스트 스토어\"},\"success\":true}")));

        // When
        ApiResponseDto<StoreResponseDto> result = storeClient.getStoreById(storeId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getData().getStoreId()).isEqualTo(storeId);

        // Verify that the X-Auth-UserId header was sent
        verify(getRequestedFor(urlEqualTo("/api/store/detail/" + storeId))
                .withHeader("X-Auth-UserId", equalTo(userId.toString())));
    }
}
