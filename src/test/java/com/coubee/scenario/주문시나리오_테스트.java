package com.coubee.scenario;

import com.coubee.data.테스트데이터_팩토리;
import com.coubee.dto.request.OrderCreateRequest;
import com.coubee.dto.response.OrderCreateResponse;
import com.coubee.dto.response.OrderDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 주문 시나리오 테스트
 * 
 * 실제 사용자가 경험할 수 있는 다양한 주문 시나리오를 테스트합니다:
 * - 일반적인 주문 플로우
 * - 주문 취소 시나리오
 * - QR 코드 픽업 시나리오
 * - 다중 상품 주문 시나리오
 * - 대량 주문 처리 시나리오
 * - 예외 상황 처리 시나리오
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("주문 시나리오 테스트")
class 주문시나리오_테스트 {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("일반적인 주문 완성 시나리오")
    @Transactional
    void 일반적인_주문완성_시나리오() throws Exception {
        // Given: 고객이 상품을 주문하려고 한다
        Long 고객ID = 1001L;
        OrderCreateRequest 주문요청 = 테스트데이터_팩토리.기본_주문요청();

        // When: 주문 생성 → 결제 완료 → QR 생성 → 수령 완료까지 전체 과정
        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 생성된주문 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 생성된주문.getOrderId();

        // 1단계: 결제 완료 처리
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk());

        // 2단계: 주문 상세 조회 및 QR 토큰 확인
        String 상세응답 = mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andReturn().getResponse().getContentAsString();

        OrderDetailResponse 주문상세 = objectMapper.readValue(상세응답, OrderDetailResponse.class);
        String QR토큰 = 주문상세.getOrderToken();

        // 3단계: QR 코드 이미지 생성
        mockMvc.perform(get("/api/qr/orders/{orderId}/image", 주문ID))
                .andExpect(status().isOk())
                .andExpected(content().contentType("image/png"));

        // 4단계: QR 토큰으로 주문 조회
        mockMvc.perform(get("/api/qr/lookup/{qrToken}", QR토큰))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.orderId").value(주문ID))
                .andExpected(jsonPath("$.status").value("PAID"));

        // 5단계: 최종 수령 처리
        mockMvc.perform(post("/api/orders/{orderId}/receive", 주문ID))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.status").value("RECEIVED"));

        // Then: 최종 상태 확인
        mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("다중 상품 주문 시나리오")
    @Transactional
    void 다중상품_주문_시나리오() throws Exception {
        // Given: 고객이 여러 상품을 함께 주문한다
        Long 고객ID = 2001L;
        OrderCreateRequest 다중상품주문 = 테스트데이터_팩토리.다중상품_주문요청();

        // When: 다중 상품 주문을 생성한다
        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(다중상품주문)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderName").value("테스트 상품 1 외 1건"))  // 2가지 상품
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 주문결과 = objectMapper.readValue(주문응답, OrderCreateResponse.class);

        // Then: 다중 상품의 총 금액이 정확히 계산되었는지 확인
        mockMvc.perform(get("/api/orders/{orderId}", 주문결과.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(300))  // 상품1 2개(200원) + 상품2 1개(100원)
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpected(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    @DisplayName("대량 주문 처리 시나리오")
    @Transactional
    void 대량주문_처리_시나리오() throws Exception {
        // Given: 고객이 같은 상품을 대량으로 주문한다
        Long 고객ID = 3001L;
        int 대량수량 = 10;
        OrderCreateRequest 대량주문 = 테스트데이터_팩토리.대량_주문요청(대량수량);

        // When: 대량 주문을 처리한다
        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(대량주문)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 주문결과 = objectMapper.readValue(주문응답, OrderCreateResponse.class);

        // Then: 대량 주문의 총 금액이 정확히 계산되었는지 확인
        mockMvc.perform(get("/api/orders/{orderId}", 주문결과.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(대량수량 * 100))  // 수량 × 단가
                .andExpected(jsonPath("$.data.recipientName").value("대량주문고객"));
    }

    @Test
    @DisplayName("주문 취소 및 환불 시나리오")
    @Transactional
    void 주문취소_및_환불_시나리오() throws Exception {
        // Given: 결제가 완료된 주문이 있다
        Long 고객ID = 4001L;
        OrderCreateRequest 취소예정주문 = 테스트데이터_팩토리.특수상황_주문요청.취소예정_주문();

        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(취소예정주문)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 주문결과 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 주문결과.getOrderId();

        // 결제 완료 상태로 변경
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk());

        // When: 고객이 주문을 취소한다
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cancelReason\":\"고객 변심으로 인한 취소\"}"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.status").value("CANCELLED"));

        // Then: 취소된 주문 상태를 확인한다
        mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("결제 수단별 주문 처리 시나리오")
    @Transactional
    void 결제수단별_주문처리_시나리오() throws Exception {
        // Given: 다양한 결제 수단으로 주문을 생성한다
        Long 고객ID_기본 = 5000L;

        // 카드 결제 주문
        OrderCreateRequest 카드주문 = 테스트데이터_팩토리.결제수단별_주문요청.카드결제_주문();
        mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID_기본 + 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(카드주문)))
                .andExpected(status().isCreated());

        // 카카오페이 주문
        OrderCreateRequest 카카오페이주문 = 테스트데이터_팩토리.결제수단별_주문요청.카카오페이_주문();
        mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID_기본 + 2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(카카오페이주문)))
                .andExpected(status().isCreated());

        // 토스페이 주문
        OrderCreateRequest 토스페이주문 = 테스트데이터_팩토리.결제수단별_주문요청.토스페이_주문();
        mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID_기본 + 3)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(토스페이주문)))
                .andExpected(status().isCreated());

        // 페이코 주문
        OrderCreateRequest 페이코주문 = 테스트데이터_팩토리.결제수단별_주문요청.페이코_주문();
        mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID_기본 + 4)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(페이코주문)))
                .andExpected(status().isCreated());

        // Then: 모든 결제 수단으로 주문이 정상 생성되었는지 확인
        mockMvc.perform(get("/api/users/{userId}/orders", 고객ID_기본 + 1))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("주문 상태 변경 시나리오")
    @Transactional
    void 주문상태_변경_시나리오() throws Exception {
        // Given: 주문이 생성된 상태
        Long 고객ID = 6001L;
        OrderCreateRequest 주문요청 = 테스트데이터_팩토리.기본_주문요청();

        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(주문요청)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 주문결과 = objectMapper.readValue(주문응답, OrderCreateResponse.class);
        String 주문ID = 주문결과.getOrderId();

        // When: 주문 상태를 단계별로 변경한다
        
        // PENDING → PAID
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.previousStatus").value("PENDING"))
                .andExpected(jsonPath("$.data.currentStatus").value("PAID"));

        // PAID → PREPARING
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.previousStatus").value("PAID"))
                .andExpected(jsonPath("$.data.currentStatus").value("PREPARING"));

        // PREPARING → PREPARED
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PREPARED\"}"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.previousStatus").value("PREPARING"))
                .andExpected(jsonPath("$.data.currentStatus").value("PREPARED"));

        // PREPARED → RECEIVED (최종 상태)
        mockMvc.perform(patch("/api/orders/{orderId}", 주문ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RECEIVED\"}"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.data.previousStatus").value("PREPARED"))
                .andExpected(jsonPath("$.data.currentStatus").value("RECEIVED"));

        // Then: 최종 상태가 RECEIVED인지 확인
        mockMvc.perform(get("/api/orders/{orderId}", 주문ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("고액 주문 처리 시나리오")
    @Transactional
    void 고액주문_처리_시나리오() throws Exception {
        // Given: 고객이 고액 주문을 생성한다
        Long 고객ID = 7001L;
        int 고액수량 = 50;  // 5000원 주문
        OrderCreateRequest 고액주문 = 테스트데이터_팩토리.특수상황_주문요청.고액_주문(고액수량);

        // When: 고액 주문을 처리한다
        String 주문응답 = mockMvc.perform(post("/api/orders")
                .header("X-User-ID", 고객ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(고액주문)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderCreateResponse 주문결과 = objectMapper.readValue(주문응답, OrderCreateResponse.class);

        // Then: 고액 주문이 정상 처리되었는지 확인
        mockMvc.perform(get("/api/orders/{orderId}", 주문결과.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(고액수량 * 100))
                .andExpected(jsonPath("$.data.recipientName").value("고액주문고객"));
    }

    @Test
    @DisplayName("동시 주문 처리 시나리오")
    @Transactional
    void 동시주문_처리_시나리오() throws Exception {
        // Given: 여러 고객이 동시에 주문을 생성한다
        int 동시주문수 = 5;
        
        // When: 동시에 여러 주문을 생성한다
        for (int i = 0; i < 동시주문수; i++) {
            Long 고객ID = 8000L + i;
            OrderCreateRequest 주문요청 = 테스트데이터_팩토리.성능테스트_데이터.동시접속_주문(i + 1);
            
            mockMvc.perform(post("/api/orders")
                    .header("X-User-ID", 고객ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(주문요청)))
                    .andExpected(status().isCreated());
        }

        // Then: 각 고객별로 주문이 정상 생성되었는지 확인
        for (int i = 0; i < 동시주문수; i++) {
            Long 고객ID = 8000L + i;
            mockMvc.perform(get("/api/users/{userId}/orders", 고객ID))
                    .andExpect(status().isOk())
                    .andExpected(jsonPath("$.data.totalCount").value(1));
        }
    }
}