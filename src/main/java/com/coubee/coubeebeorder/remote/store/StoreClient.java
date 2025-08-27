package com.coubee.coubeebeorder.remote.store;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Store 서비스와 통신하기 위한 Feign 클라이언트
 *
 * Spring Cloud의 서비스 디스커버리를 활용하여 Store 서비스의 위치를 동적으로 찾습니다.
 * 하드코딩된 URL 대신 논리적 서비스명을 사용하여 유연성과 확장성을 제공합니다.
 *
 * X-Auth-UserId 헤더는 Store 서비스 엔드포인트에서 필수로 요구되며,
 * 원본 요청에서 전달받아 포워딩해야 합니다.
 */
@FeignClient(
    name = "coubee-be-store-service",
    url = "http://coubee-be-store-service:8080",
    configuration = com.coubee.coubeebeorder.config.FeignConfig.class
)
public interface StoreClient {

    /**
     * 스토어 ID로 스토어 상세 정보 조회
     *
     * @param storeId 조회할 스토어의 ID
     * @param userId X-Auth-UserId 헤더의 사용자 ID (Store 서비스에서 필수)
     * @return StoreResponseDto를 포함한 ApiResponseDto
     */
    @GetMapping("/api/store/detail/{storeId}")
    ApiResponseDto<StoreResponseDto> getStoreById(
            @PathVariable("storeId") Long storeId,
            @RequestHeader("X-Auth-UserId") Long userId
    );

    /**
     * 스토어 ID로 스토어 이름만 조회
     *
     * @param storeId 조회할 스토어의 ID
     * @param userId X-Auth-UserId 헤더의 사용자 ID (Store 서비스에서 필수)
     * @return 스토어 이름을 포함한 ApiResponseDto
     */
    @GetMapping("/api/store/{storeId}/name")
    ApiResponseDto<String> getStoreNameById(
            @PathVariable("storeId") Long storeId,
            @RequestHeader("X-Auth-UserId") Long userId
    );

    /**
     * 여러 스토어 ID로 스토어 상세 정보 일괄 조회
     * N+1 문제 해결을 위한 벌크 조회 API
     *
     * @param storeIds 조회할 스토어 ID 목록
     * @param userId X-Auth-UserId 헤더의 사용자 ID (Store 서비스에서 필수)
     * @return 스토어 ID를 키로 하는 StoreResponseDto 맵을 포함한 ApiResponseDto
     */
    @GetMapping("/api/store/bulk")
    ApiResponseDto<Map<Long, StoreResponseDto>> getStoresByIds(
            @RequestParam("storeIds") List<Long> storeIds,
            @RequestHeader("X-Auth-UserId") Long userId
    );
}
