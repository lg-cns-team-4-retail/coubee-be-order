package com.coubee.coubeebeorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "테스트용 결제 완료 주문 생성을 위한 요청 DTO (translation: Request DTO for creating a test order with completed payment)")
public class TestOrderCreateRequest {

    @NotNull(message = "사용자 ID는 필수입니다. (translation: User ID is required.)")
    @Schema(description = "사용자 ID (translation: User ID)", example = "1")
    private Long userId;

    @NotNull(message = "매장 ID는 필수입니다. (translation: Store ID is required.)")
    @Schema(description = "매장 ID (translation: Store ID)", example = "1")
    private Long storeId;

    @NotNull(message = "상품 ID는 필수입니다. (translation: Product ID is required.)")
    @Schema(description = "상품 ID (translation: Product ID)", example = "5066")
    private Long productId;

    @NotNull(message = "수량은 필수입니다. (translation: Quantity is required.)")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다. (translation: Quantity must be 1 or greater.)")
    @Schema(description = "주문 수량 (translation: Order quantity)", example = "2")
    private Integer quantity;
}
