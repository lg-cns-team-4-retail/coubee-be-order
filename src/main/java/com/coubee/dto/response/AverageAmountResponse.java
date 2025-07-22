package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AverageAmountResponse(
        @Schema(description = "Overall average purchase amount per person", example = "30000") Integer totalAverageAmount,
        @Schema(description = "Today's average purchase amount per person", example = "5000") Integer todayAverageAmount) {
    public static AverageAmountResponse of(Integer totalAverageAmount, Integer todayAverageAmount) {
        return new AverageAmountResponse(totalAverageAmount, todayAverageAmount);
    }
} 
