package com.coubee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

/**
 * Order Cancellation Request DTO
 */
@Getter
@Builder
public class OrderCancelRequest {

    @Schema(description = "Cancellation reason", example = "Cancelled by customer request")
    @NotBlank(message = "Cancellation reason is required")
    private String cancelReason;
} 
