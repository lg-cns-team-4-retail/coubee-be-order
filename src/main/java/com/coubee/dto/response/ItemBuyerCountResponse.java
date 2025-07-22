package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ItemBuyerCountResponse(
        @Schema(description = "Item ID", example = "1") Long itemId,
        @Schema(description = "Buyer count", example = "42") long buyerCount) {} 
