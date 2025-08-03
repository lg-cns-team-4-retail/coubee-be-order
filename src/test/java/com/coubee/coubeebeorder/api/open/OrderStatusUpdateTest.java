package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateResponse;
import com.coubee.coubeebeorder.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderStatusUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("주문 상태 업데이트 - 성공 (Store Owner)")
    void updateOrderStatus_Success_StoreOwner() throws Exception {
        // Given
        String orderId = "order_test_123";
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.PREPARING)
                .reason("Started food preparation")
                .build();

        OrderStatusUpdateResponse response = OrderStatusUpdateResponse.builder()
                .orderId(orderId)
                .previousStatus(OrderStatus.PAID)
                .currentStatus(OrderStatus.PREPARING)
                .reason("Started food preparation")
                .updatedAt(LocalDateTime.now())
                .updatedByUserId(123L)
                .build();

        given(orderService.updateOrderStatus(eq(orderId), any(OrderStatusUpdateRequest.class), eq(123L)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/order/orders/{orderId}", orderId)
                        .header("X-Auth-UserId", "123")
                        .header("X-Auth-Role", "STORE_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order status has been updated"))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.previousStatus").value("PAID"))
                .andExpect(jsonPath("$.data.currentStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.reason").value("Started food preparation"))
                .andExpect(jsonPath("$.data.updatedByUserId").value(123));
    }

    @Test
    @DisplayName("주문 상태 업데이트 - 성공 (Admin)")
    void updateOrderStatus_Success_Admin() throws Exception {
        // Given
        String orderId = "order_test_123";
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.CANCELLED)
                .reason("Customer request")
                .build();

        OrderStatusUpdateResponse response = OrderStatusUpdateResponse.builder()
                .orderId(orderId)
                .previousStatus(OrderStatus.PREPARING)
                .currentStatus(OrderStatus.CANCELLED)
                .reason("Customer request")
                .updatedAt(LocalDateTime.now())
                .updatedByUserId(456L)
                .build();

        given(orderService.updateOrderStatus(eq(orderId), any(OrderStatusUpdateRequest.class), eq(456L)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/order/orders/{orderId}", orderId)
                        .header("X-Auth-UserId", "456")
                        .header("X-Auth-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("주문 상태 업데이트 - 실패 (권한 없음)")
    void updateOrderStatus_Fail_Unauthorized() throws Exception {
        // Given
        String orderId = "order_test_123";
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.PREPARING)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/order/orders/{orderId}", orderId)
                        .header("X-Auth-UserId", "123")
                        .header("X-Auth-Role", "USER")  // Regular user, not store owner
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("주문 상태 업데이트 - 실패 (필수 필드 누락)")
    void updateOrderStatus_Fail_MissingStatus() throws Exception {
        // Given
        String orderId = "order_test_123";
        String invalidRequest = "{\"reason\":\"Test reason\"}"; // Missing status

        // When & Then
        mockMvc.perform(patch("/api/order/orders/{orderId}", orderId)
                        .header("X-Auth-UserId", "123")
                        .header("X-Auth-Role", "STORE_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("주문 상태 업데이트 - 실패 (인증 헤더 누락)")
    void updateOrderStatus_Fail_MissingAuthHeaders() throws Exception {
        // Given
        String orderId = "order_test_123";
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.PREPARING)
                .build();

        // When & Then - Missing X-Auth-UserId header
        mockMvc.perform(patch("/api/order/orders/{orderId}", orderId)
                        .header("X-Auth-Role", "STORE_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
