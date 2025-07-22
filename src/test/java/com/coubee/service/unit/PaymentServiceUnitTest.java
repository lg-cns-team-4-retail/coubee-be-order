package com.coubee.service.unit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.coubee.domain.Payment;
import com.coubee.domain.PaymentStatus;
import com.coubee.dto.FlatPaymentItem;
import com.coubee.dto.request.PaymentReadyRequest;
import com.coubee.dto.response.*;
import com.coubee.error.exception.CustomException;
import com.coubee.exception.PaymentErrorCode;
import com.coubee.kafka.producer.ItemPurchasedProducer;
import com.coubee.repository.PaymentRepository;
import com.coubee.response.SliceResponse;
import com.coubee.service.PaymentServiceImpl;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUnitTest {

    @InjectMocks private PaymentServiceImpl paymentService;
    @Mock private PaymentRepository paymentRepository;


    @Mock private IamportClient iamportClient;
    @Mock private IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock private com.siot.IamportRestClient.response.Payment iamportPayment;
    @Mock private ItemPurchasedProducer itemPurchasedProducer;

    @Nested
    class WhenPreparingPayment {

        @Test
        void shouldRespondWithPaymentInfoForValidRequest() {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(17L, 3)));

            stubItemDetails();

            // when
            PaymentReadyResponse response = paymentService.preparePayment("1", request);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.buyerName()).isEqualTo("User 1"),
                    () -> assertThat(response.name()).isEqualTo("피규어 지수 외 1건"),
                    () -> assertThat(response.amount()).isEqualTo(129000),
                    () ->
                            assertThat(response.merchantUid())
                                    .matches("^popup_1_order_[0-9a-fA-F\\-]{16}$"));
        }

        @Test
        void shouldThrowExceptionWhenItemIdDoesNotExist() {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(999L, 3)));

            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.ITEM_NOT_FOUND.getMessage());
        }

        @Test
        void shouldThrowOutOfStockExceptionWhenQuantityExceedsStock() {
            // given
            PaymentReadyRequest request =
                    new PaymentReadyRequest(
                            1L,
                            List.of(
                                    new PaymentReadyRequest.Item(9L, 1),
                                    new PaymentReadyRequest.Item(17L, 12)));

            stubItemDetails();

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment("1", request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.OUT_OF_STOCK.getMessage());
        }

        private void stubFindMember() {
            // Member service call removed - using userId directly
        }

        private void stubItemDetails() {
            // Manager service client removed - using direct values
        }
    }

    @Nested
    class WhenVerifyingPayment {

        @Test
        void shouldUpdatePaymentInfoWhenImpUidMatchesPaymentInfo() throws IOException, IamportResponseException {
            // given
            Payment payment = Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L);
            given(paymentRepository.findByMerchantUid("popup_1_order_test-uuid"))
                    .willReturn(Optional.of(payment));

            stubIamportResponse(BigDecimal.valueOf(129000), "PAID");

            // when
            paymentService.findPaymentByImpUid("testImpUid");

            // then
            Assertions.assertAll(
                    () -> assertThat(payment.getAmount()).isEqualTo(129000),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID),
                    () -> assertThat(payment.getMerchantUid()).isEqualTo("popup_1_order_test-uuid"),
                    () -> assertThat(payment.getPgProvider()).isEqualTo("tosspay"));
        }

        @Test
        void shouldThrowExceptionWhenPaymentAmountDiffers() throws IOException, IamportResponseException {
            // given
            Payment payment = Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L);
            given(paymentRepository.findByMerchantUid("popup_1_order_test-uuid"))
                    .willReturn(Optional.of(payment));

            stubIamportResponse(BigDecimal.valueOf(1000), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.INVALID_AMOUNT.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPaymentStatusIsNotPaid() throws IOException, IamportResponseException {
            // given
            Payment payment = Payment.createPayment(1L, "popup_1_order_test-uuid", 129000, 1L);
            given(paymentRepository.findByMerchantUid("popup_1_order_test-uuid"))
                    .willReturn(Optional.of(payment));

            stubIamportResponse(BigDecimal.valueOf(129000), "READY");

            // when & then
            assertThatThrownBy(() -> paymentService.findPaymentByImpUid("testImpUid"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
        }

        private void stubIamportResponse(BigDecimal amount, String status)
                throws IOException, IamportResponseException {
            given(iamportClient.paymentByImpUid(anyString())).willReturn(iamportResponse);
            given(iamportResponse.getResponse()).willReturn(iamportPayment);

            given(iamportPayment.getMerchantUid()).willReturn("popup_1_order_test-uuid");
            given(iamportPayment.getPgProvider()).willReturn("tosspay");
            given(iamportPayment.getAmount()).willReturn(amount);
            given(iamportPayment.getStatus()).willReturn(status);
            given(iamportPayment.getPaidAt()).willReturn(new Date());
        }
    }

    @Nested
    class WhenHandlingItemBuyerCountRequests {

        @Test
        void shouldRetrieveItemBuyerCountCorrectly() {
            // given
            Long popupId = 1L;

            given(paymentRepository.countItemBuyerByPopupId(popupId))
                    .willReturn(
                            List.of(
                                    new ItemBuyerCountResponse(1L, 2),
                                    new ItemBuyerCountResponse(2L, 1),
                                    new ItemBuyerCountResponse(3L, 1)));

            // when
            List<ItemBuyerCountResponse> result = paymentService.countItemBuyerByPopupId(popupId);

            // then
            Assertions.assertAll(
                    () -> assertThat(result.get(0).itemId()).isEqualTo(1L),
                    () -> assertThat(result.get(0).buyerCount()).isEqualTo(2),
                    () -> assertThat(result.get(1).itemId()).isEqualTo(2L),
                    () -> assertThat(result.get(1).buyerCount()).isEqualTo(1),
                    () -> assertThat(result.get(2).itemId()).isEqualTo(3L),
                    () -> assertThat(result.get(2).buyerCount()).isEqualTo(1));
        }
    }

    @Nested
    class WhenRetrievingPaymentHistory {

        @Test
        void shouldRetrievePaymentHistoryWithItemsCorrectly() {
            // given
            List<FlatPaymentItem> flatItems =
                    List.of(
                            new FlatPaymentItem(
                                    1L, 1L, LocalDateTime.of(2024, 5, 31, 14, 0), "응원봉", 1, 25000),
                            new FlatPaymentItem(
                                    1L, 1L, LocalDateTime.of(2024, 5, 31, 14, 0), "포스터", 3, 9000),
                            new FlatPaymentItem(
                                    2L,
                                    2L,
                                    LocalDateTime.of(2024, 5, 30, 15, 30),
                                    "크레용 파란색",
                                    1,
                                    12000));

            Slice<FlatPaymentItem> slice = new SliceImpl<>(flatItems, PageRequest.of(0, 10), false);

            given(paymentRepository.findAllPaymentHistoryByMemberId(anyLong(), any(), anyInt()))
                    .willReturn(slice);

            // when
            SliceResponse<PaymentHistoryResponse> response =
                    paymentService.findAllPaymentHistory("1", null, 10);

            // then
            List<PaymentHistoryResponse> content = response.content();

            PaymentHistoryResponse first = content.get(0);
            Assertions.assertAll(
                    () -> assertThat(first.paymentId()).isEqualTo(1L),
                    () -> assertThat(first.popupId()).isEqualTo(1L),
                    () -> assertThat(first.items().get(0).itemName()).isEqualTo("응원봉"),
                    () -> assertThat(first.items().get(1).itemName()).isEqualTo("포스터"),
                    () -> assertThat(first.items().get(0).price()).isEqualTo(25000),
                    () -> assertThat(first.items().get(1).price()).isEqualTo(27000));

            PaymentHistoryResponse second = content.get(1);
            Assertions.assertAll(
                    () -> assertThat(second.paymentId()).isEqualTo(2L),
                    () -> assertThat(second.popupId()).isEqualTo(2L),
                    () -> assertThat(second.items().get(0).itemName()).isEqualTo("크레용 파란색"),
                    () -> assertThat(second.items().get(0).price()).isEqualTo(12000));
        }
    }

    @Nested
    class WhenHandlingAverageAmountRequests {

        @Test
        void shouldRetrieveAverageAmountCorrectly() {
            // given
            Long popupId = 1L;

            given(paymentRepository.findAverageAmountByPopupId(anyLong()))
                    .willReturn(new AverageAmountResponse(31333, 24500));

            // when
            AverageAmountResponse response = paymentService.findAverageAmount(popupId);

            // then
            assertThat(response.totalAverageAmount()).isEqualTo(31333);
            assertThat(response.todayAverageAmount()).isEqualTo(24500);
        }
    }
}
