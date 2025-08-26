package com.coubee.coubeebeorder.remote.product;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Product 서비스와 통신하기 위한 Feign 클라이언트
 *
 * Spring Cloud의 서비스 디스커버리를 활용하여 Product 서비스의 위치를 동적으로 찾습니다.
 * 하드코딩된 URL 대신 논리적 서비스명을 사용하여 유연성과 확장성을 제공합니다.
 *
 * X-Auth-UserId 헤더는 Product 서비스 엔드포인트에서 필수로 요구되며,
 * 원본 요청에서 전달받아 포워딩해야 합니다.
 */
@FeignClient(
    name = "coubee-be-product-service",
    url = "http://coubee-be-product-service:8080",
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
}
