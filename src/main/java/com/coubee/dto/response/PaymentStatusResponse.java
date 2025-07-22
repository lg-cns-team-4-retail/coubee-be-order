package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentStatusResponse {
    
    @Schema(description = "Payment ID", example = "PAY_202305151234")
    private String paymentId;
    
    @Schema(description = "Payment status", example = "PAID")
    private String status;
    
    @Schema(description = "Payment amount", example = "15000")
    private Integer amount;
    
    @Schema(description = "Payment date time")
    private LocalDateTime paidAt;
    
    @Schema(description = "Receipt URL")
    private String receiptUrl;
} 