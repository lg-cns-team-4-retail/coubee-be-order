package com.coubee.coubeebeorder.remote.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StoreResponseDto 테스트
 */
class StoreResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("StoreResponseDto 기본 생성 및 필드 설정")
    void createStoreResponseDto() {
        // Given & When
        StoreResponseDto dto = new StoreResponseDto();
        dto.setStoreId(1L);
        dto.setStoreName("테스트 스토어");
        dto.setDescription("테스트 스토어 설명");
        dto.setStoreImg("https://example.com/store.jpg");
        dto.setAddress("서울시 강남구 테스트로 123");
        dto.setPhoneNumber("02-1234-5678");
        dto.setBusinessHours("09:00-22:00");
        dto.setCategory("카페");
        dto.setOwnerId(50L);

        // Then
        assertThat(dto.getStoreId()).isEqualTo(1L);
        assertThat(dto.getStoreName()).isEqualTo("테스트 스토어");
        assertThat(dto.getDescription()).isEqualTo("테스트 스토어 설명");
        assertThat(dto.getStoreImg()).isEqualTo("https://example.com/store.jpg");
        assertThat(dto.getAddress()).isEqualTo("서울시 강남구 테스트로 123");
        assertThat(dto.getPhoneNumber()).isEqualTo("02-1234-5678");
        assertThat(dto.getBusinessHours()).isEqualTo("09:00-22:00");
        assertThat(dto.getCategory()).isEqualTo("카페");
        assertThat(dto.getOwnerId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("StoreResponseDto JSON 직렬화")
    void jsonSerialization() throws Exception {
        // Given
        StoreResponseDto dto = new StoreResponseDto();
        dto.setStoreId(1L);
        dto.setStoreName("테스트 스토어");
        dto.setDescription("테스트 스토어 설명");
        dto.setStoreImg("https://example.com/store.jpg");
        dto.setAddress("서울시 강남구 테스트로 123");
        dto.setPhoneNumber("02-1234-5678");
        dto.setBusinessHours("09:00-22:00");
        dto.setCategory("카페");
        dto.setOwnerId(50L);

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"storeId\":1");
        assertThat(json).contains("\"storeName\":\"테스트 스토어\"");
        assertThat(json).contains("\"description\":\"테스트 스토어 설명\"");
        assertThat(json).contains("\"storeImg\":\"https://example.com/store.jpg\"");
        assertThat(json).contains("\"address\":\"서울시 강남구 테스트로 123\"");
        assertThat(json).contains("\"phoneNumber\":\"02-1234-5678\"");
        assertThat(json).contains("\"businessHours\":\"09:00-22:00\"");
        assertThat(json).contains("\"category\":\"카페\"");
        assertThat(json).contains("\"ownerId\":50");
    }

    @Test
    @DisplayName("StoreResponseDto JSON 역직렬화")
    void jsonDeserialization() throws Exception {
        // Given
        String json = """
                {
                    "storeId": 1,
                    "storeName": "테스트 스토어",
                    "description": "테스트 스토어 설명",
                    "storeImg": "https://example.com/store.jpg",
                    "address": "서울시 강남구 테스트로 123",
                    "phoneNumber": "02-1234-5678",
                    "businessHours": "09:00-22:00",
                    "category": "카페",
                    "ownerId": 50
                }
                """;

        // When
        StoreResponseDto dto = objectMapper.readValue(json, StoreResponseDto.class);

        // Then
        assertThat(dto.getStoreId()).isEqualTo(1L);
        assertThat(dto.getStoreName()).isEqualTo("테스트 스토어");
        assertThat(dto.getDescription()).isEqualTo("테스트 스토어 설명");
        assertThat(dto.getStoreImg()).isEqualTo("https://example.com/store.jpg");
        assertThat(dto.getAddress()).isEqualTo("서울시 강남구 테스트로 123");
        assertThat(dto.getPhoneNumber()).isEqualTo("02-1234-5678");
        assertThat(dto.getBusinessHours()).isEqualTo("09:00-22:00");
        assertThat(dto.getCategory()).isEqualTo("카페");
        assertThat(dto.getOwnerId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("StoreResponseDto null 값 처리")
    void handleNullValues() {
        // Given & When
        StoreResponseDto dto = new StoreResponseDto();
        // 모든 필드를 null로 유지

        // Then
        assertThat(dto.getStoreId()).isNull();
        assertThat(dto.getStoreName()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getStoreImg()).isNull();
        assertThat(dto.getAddress()).isNull();
        assertThat(dto.getPhoneNumber()).isNull();
        assertThat(dto.getBusinessHours()).isNull();
        assertThat(dto.getCategory()).isNull();
        assertThat(dto.getOwnerId()).isNull();
    }

    @Test
    @DisplayName("StoreResponseDto equals 및 hashCode 테스트")
    void equalsAndHashCode() {
        // Given
        StoreResponseDto dto1 = new StoreResponseDto();
        dto1.setStoreId(1L);
        dto1.setStoreName("테스트 스토어");
        dto1.setAddress("서울시 강남구");

        StoreResponseDto dto2 = new StoreResponseDto();
        dto2.setStoreId(1L);
        dto2.setStoreName("테스트 스토어");
        dto2.setAddress("서울시 강남구");

        StoreResponseDto dto3 = new StoreResponseDto();
        dto3.setStoreId(2L);
        dto3.setStoreName("다른 스토어");
        dto3.setAddress("서울시 서초구");

        // Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1).isNotEqualTo(dto3);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        assertThat(dto1.hashCode()).isNotEqualTo(dto3.hashCode());
    }

    @Test
    @DisplayName("StoreResponseDto toString 테스트")
    void toStringTest() {
        // Given
        StoreResponseDto dto = new StoreResponseDto();
        dto.setStoreId(1L);
        dto.setStoreName("테스트 스토어");
        dto.setCategory("카페");

        // When
        String toString = dto.toString();

        // Then
        assertThat(toString).contains("StoreResponseDto");
        assertThat(toString).contains("storeId=1");
        assertThat(toString).contains("storeName=테스트 스토어");
        assertThat(toString).contains("category=카페");
    }
}
