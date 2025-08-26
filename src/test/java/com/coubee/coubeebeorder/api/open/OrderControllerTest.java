package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusResponse;
import com.coubee.coubeebeorder.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("주문 상태 조회 - 성공")
    void getOrderStatus_Success() throws Exception {
        // Given
        String orderId = "order_test_123";
        OrderStatusResponse response = OrderStatusResponse.builder()
                .orderId(orderId)
                .status(OrderStatus.PAID)
                .build();

        given(orderService.getOrderStatus(anyString())).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/order/orders/status/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("주문 상태 조회 - 다양한 상태값 테스트")
    void getOrderStatus_DifferentStatuses() throws Exception {
        // Test PENDING status
        String orderId = "order_pending_123";
        OrderStatusResponse pendingResponse = OrderStatusResponse.builder()
                .orderId(orderId)
                .status(OrderStatus.PENDING)
                .build();

        given(orderService.getOrderStatus(orderId)).willReturn(pendingResponse);

        mockMvc.perform(get("/api/order/orders/status/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // Test PREPARING status
        String preparingOrderId = "order_preparing_123";
        OrderStatusResponse preparingResponse = OrderStatusResponse.builder()
                .orderId(preparingOrderId)
                .status(OrderStatus.PREPARING)
                .build();

        given(orderService.getOrderStatus(preparingOrderId)).willReturn(preparingResponse);

        mockMvc.perform(get("/api/order/orders/status/{orderId}", preparingOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"));

        // Test CANCELLED status
        String cancelledOrderId = "order_cancelled_123";
        OrderStatusResponse cancelledResponse = OrderStatusResponse.builder()
                .orderId(cancelledOrderId)
                .status(OrderStatus.CANCELLED)
                .build();

        given(orderService.getOrderStatus(cancelledOrderId)).willReturn(cancelledResponse);

        mockMvc.perform(get("/api/order/orders/status/{orderId}", cancelledOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("내 주문 목록 조회 - 성공")
    void getMyOrders_Success() throws Exception {
        // Given
        Long userId = 1L;
        OrderDetailResponse orderDetail = OrderDetailResponse.builder()
                .orderId("order_test_123")
                .userId(userId)
                .storeId(1L)
                .status(OrderStatus.PAID)
                .totalAmount(25000)
                .recipientName("홍길동")
                .createdAt(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();

        List<OrderDetailResponse> orders = List.of(orderDetail);
        Page<OrderDetailResponse> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

        given(orderService.getUserOrders(eq(userId), any(PageRequest.class))).willReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/api/order/users/me/orders")
                        .header("X-Auth-UserId", userId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].orderId").value("order_test_123"))
                .andExpect(jsonPath("$.data.content[0].userId").value(userId))
                .andExpect(jsonPath("$.data.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }
}
