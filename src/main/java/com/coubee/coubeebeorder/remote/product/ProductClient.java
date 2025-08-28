package com.coubee.coubeebeorder.remote.product;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Product 서비스와 통신하기 위한 Feign 클라이언트
 *
 * Spring Cloud의 서비스 디스커버리를 활용하여 Product 서비스의 위치를 동적으로 찾습니다.
 * 하드코딩된 URL 대신 논리적 서비스명을 사용하여 유연성과 확장성을 제공합니다.
 *
 * X-Auth-UserId 헤더는 Product 서비스 엔드포인트에서 필수로 요구되며,
 * 원본 요청에서 전달받아 포워딩해야 합니다.
 *
 * 이 클라이언트는 상품 조회와 재고 관리 기능을 모두 제공합니다.
 */
@FeignClient(
    name = "coubee-be-product-service",
    configuration = com.coubee.coubeebeorder.config.FeignConfig.class
)
public interface ProductClient {

    /**
     * 상품 ID로 상품 상세 정보 조회
     *
     * @param productId 조회할 상품의 ID
     * @param userId X-Auth-UserId 헤더의 사용자 ID (Product 서비스에서 필수)
     * @return ProductResponseDto를 포함한 ApiResponseDto
     */
    @GetMapping("/api/product/detail/{productId}")
    ApiResponseDto<ProductResponseDto> getProductById(
            @PathVariable("productId") Long productId,
            @RequestHeader("X-Auth-UserId") Long userId
    );

    /**
     * 재고 업데이트 (감소/증가)
     *
     * @param request 재고 업데이트 요청 정보 (storeId와 상품별 변경량 포함)
     * @param userId X-Auth-UserId 헤더의 사용자 ID (Product 서비스에서 필수)
     * @return StockUpdateResponse를 포함한 ApiResponseDto
     */
    @PostMapping("/backend/product/stock/update")
    ApiResponseDto<StockUpdateResponse> updateStock(
            @RequestBody StockUpdateRequest request,
            @RequestHeader("X-Auth-UserId") Long userId
    );

    /**
     * 여러 상품 ID로 상품 상세 정보 일괄 조회
     * N+1 문제 해결을 위한 벌크 조회 API
     *
     * @param productIds 조회할 상품 ID 목록
     * @param userId X-Auth-UserId 헤더의 사용자 ID (Product 서비스에서 필수)
     * @return 상품 ID를 키로 하는 ProductResponseDto 맵을 포함한 ApiResponseDto
     */
    @GetMapping("/backend/product/bulk")
    ApiResponseDto<Map<Long, ProductResponseDto>> getProductsByIds(
            @RequestParam("productIds") List<Long> productIds,
            @RequestHeader("X-Auth-UserId") Long userId
    );
}
