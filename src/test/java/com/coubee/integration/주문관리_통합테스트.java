package com.coubee.integration;

import com.coubee.dto.request.OrderCreateRequest;
import com.coubee.dto.response.OrderCreateResponse;
import com.coubee.dto.response.OrderDetailResponse;
import com.coubee.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 주문 관리 시스템 통합 테스트
 * 
 * 주문 생성부터 수령까지의 전체 플로우를 테스트합니다:
 * - 주문 생성 및 결제 준비
 * - 주문 상세 조회
 * - 주문 목록 조회
 * - QR 코드 생성 및 조회
 * - 주문 수령 처리
 * - 주문 상태 변경
 * - 주문 취소 및 환불
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("주문 관리 시스템 통합 테스트")
class 주문관리_통합테스트 {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("coubee_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void 데이터베이스_설정(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    @DisplayName("주문 생성 및 결제 준비 테스트")
    @Transactional
    void 주문_생성_및_결제준비_테스트() throws Exception {
        // Given: 사용자가 상품을 주문한다
        Long 사용자ID = 12345L;
        Long 매장ID = 1L;
        
        OrderCreateRequest 주문요청 = OrderCreateRequest.builder()
                .storeId(매장ID)
                .recipientName("홍길동")
                .paymentMethod("KAKAOPAY")
                .items(List.of(
                    OrderCreateRequest.OrderItemRequest.builder()
                        .productId(1L)
                        .quantity(2)
                        .build()
                ))
                .build();

        // When: 주문을 생성한다
        String 응답내용 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderName").value("테스트 상품 1"))
                .andExpect(jsonPath("$.data.buyerName").value("홍길동"))
                .andExpect(jsonPath("$.data.amount").value(200))
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 생성된주문 = objectMapper.readValue(응답내용, OrderCreateResponse.class);

        // Then: 생성된 주문이 올바른지 검증한다
        mockMvc.perform(get("/api/orders/{orderId}", 생성된주문.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.recipientName").value("홍길동"))
                .andExpect(jsonPath("$.data.totalAmount").value(200));
    }

    @Test
    @DisplayName("주문 목록 조회 테스트")
    @Transactional
    void 주문_목록_조회_테스트() throws Exception {
        // Given: 사용자가 여러 개의 주문을 생성한다
        Long 사용자ID = 67890L;
        
        // 첫 번째 주문 생성
        OrderCreateRequest 첫번째주문 = OrderCreateRequest.builder()
                .storeId(1L)
                .recipientName("김철수")
                .paymentMethod("CARD")
                .items(List.of(OrderCreateRequest.OrderItemRequest.builder()
                        .productId(1L).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(첫번째주문)))
                .andExpect(status().isCreated());

        // 두 번째 주문 생성
        OrderCreateRequest 두번째주문 = OrderCreateRequest.builder()
                .storeId(1L)
                .recipientName("김철수")
                .paymentMethod("TOSSPAY")
                .items(List.of(OrderCreateRequest.OrderItemRequest.builder()
                        .productId(2L).quantity(3).build()))
                .build();

        mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(두번째주문)))
                .andExpect(status().isCreated());

        // When: 사용자의 주문 목록을 조회한다
        mockMvc.perform(get("/api/users/{userId}/orders?page=0&size=10", 사용자ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders").isArray())
                .andExpect(jsonPath("$.data.orders.length()").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.currentPage").value(0));
    }

    @Test
    @DisplayName("QR 코드 생성 및 조회 테스트")
    @Transactional
    void QR코드_생성_및_조회_테스트() throws Exception {
        // Given: 주문이 생성되고 결제가 완료된 상태
        Long 사용자ID = 11111L;
        
        OrderCreateRequest 주문요청 = OrderCreateRequest.builder()
                .storeId(1L)
                .recipientName("박영희")
                .paymentMethod("KAKAOPAY")
                .items(List.of(OrderCreateRequest.OrderItemRequest.builder()
                        .productId(1L).quantity(1).build()))
                .build();

        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 생성된주문 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 생성된주문.getOrderId();

        // 결제 완료 상태로 변경
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk());

        // When: QR 코드 이미지를 생성한다
        mockMvc.perform(get("/api/qr/orders/{orderId}/image", 주문ID))
                .andExpect(status().isOk())
                .andExpected(content().contentType("image/png"));

        // QR 토큰으로 주문 조회
        String 주문상세응답 = mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderDetailResponse 주문상세 = objectMapper.readValue(주문상세응답, OrderDetailResponse.class);
        String QR토큰 = 주문상세.getOrderToken();

        // Then: QR 토큰으로 주문 정보를 조회할 수 있다
        mockMvc.perform(get("/api/qr/lookup/{qrToken}", QR토큰))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.orderId").value(주문ID))
                .andExpected(jsonPath("$.recipientName").value("박영희"));
    }

    @Test
    @DisplayName("주문 수령 처리 테스트")
    @Transactional
    void 주문_수령_처리_테스트() throws Exception {
        // Given: 결제가 완료된 주문이 있다
        Long 사용자ID = 22222L;
        
        OrderCreateRequest 주문요청 = OrderCreateRequest.builder()
                .storeId(1L)
                .recipientName("이민수")
                .paymentMethod("CARD")
                .items(List.of(OrderCreateRequest.OrderItemRequest.builder()
                        .productId(1L).quantity(1).build()))
                .build();

        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 생성된주문 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 생성된주문.getOrderId();

        // 결제 완료 상태로 변경
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk());

        // When: 고객이 주문을 수령한다
        mockMvc.perform(post("/api/orders/{orderId}/receive", 주문ID))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.status").value("RECEIVED"))
                .andExpected(jsonPath("$.message").value("Order has been received"));

        // Then: 주문 상태가 RECEIVED로 변경되었는지 확인한다
        mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("주문 상태 수동 변경 테스트")
    @Transactional
    void 주문상태_수동변경_테스트() throws Exception {
        // Given: 주문이 생성된 상태
        Long 사용자ID = 33333L;
        
        OrderCreateRequest 주문요청 = OrderCreateRequest.builder()
                .storeId(1L)
                .recipientName("정다은")
                .paymentMethod("TOSSPAY")
                .items(List.of(OrderCreateRequest.OrderItemRequest.builder()
                        .productId(1L).quantity(1).build()))
                .build();

        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 생성된주문 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 생성된주문.getOrderId();

        // When: 관리자가 주문 상태를 수동으로 변경한다
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.currentStatus").value("PREPARING"))
                .andExpected(jsonPath("$.data.orderId").value(주문ID));

        // Then: 주문 상태가 올바르게 변경되었는지 확인한다
        mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"));
    }

    @Test
    @DisplayName("주문 취소 및 환불 처리 테스트")
    @Transactional
    void 주문_취소_및_환불처리_테스트() throws Exception {
        // Given: 결제가 완료된 주문이 있다
        Long 사용자ID = 44444L;
        
        OrderCreateRequest 주문요청 = OrderCreateRequest.builder()
                .storeId(1L)
                .recipientName("최민준")
                .paymentMethod("KAKAOPAY")
                .items(List.of(OrderCreateRequest.OrderItemRequest.builder()
                        .productId(1L).quantity(1).build()))
                .build();

        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 사용자ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 생성된주문 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 생성된주문.getOrderId();

        // 결제 완료 상태로 변경
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk());

        // When: 고객이 주문을 취소한다
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cancelReason\":\"고객 요청에 의한 취소\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpected(jsonPath("$.message").value("Order has been cancelled"));

        // Then: 주문이 취소 상태로 변경되었는지 확인한다
        mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("결제 설정 정보 조회 테스트")
    @Transactional
    void 결제설정정보_조회_테스트() throws Exception {
        // When: 결제 설정 정보를 조회한다
        mockMvc.perform(get("/api/payments/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storeId").exists())
                .andExpect(jsonPath("$.data.channelKeys").exists())
                .andExpect(jsonPath("$.data.channelKeys.card").exists())
                .andExpect(jsonPath("$.data.channelKeys.kakaopay").exists())
                .andExpect(jsonPath("$.data.channelKeys.tosspay").exists())
                .andExpected(jsonPath("$.data.channelKeys.payco").exists());
    }
}