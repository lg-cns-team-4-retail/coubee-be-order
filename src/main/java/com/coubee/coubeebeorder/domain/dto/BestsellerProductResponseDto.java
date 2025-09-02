package com.coubee.coubeebeorder.domain.dto;

import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BestsellerProductResponseDto {

    @Schema(description = "Total quantity sold (this value is used for sorting)", example = "50")
    private Long totalQuantitySold;

    @JsonUnwrapped // This annotation flattens the productDetails fields into the parent JSON object.
    private ProductResponseDto productDetails;
}
