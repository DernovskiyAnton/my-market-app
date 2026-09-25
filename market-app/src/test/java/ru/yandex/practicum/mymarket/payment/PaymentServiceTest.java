package ru.yandex.practicum.mymarket.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.client.model.Balance;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentRequest;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentResult;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentsApi paymentsApi;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentsApi,
                new PaymentProperties("http://localhost", Duration.ofMillis(200)));
    }

    @Test
    void getBalance_returnsAmount() {
        when(paymentsApi.getBalance()).thenReturn(Mono.just(new Balance(5000L)));

        StepVerifier.create(paymentService.getBalance())
                .expectNext(5000L)
                .verifyComplete();
    }

    @Test
    void checkAvailability_enoughFunds_allowsPurchase() {
        when(paymentsApi.getBalance()).thenReturn(Mono.just(new Balance(5000L)));

        StepVerifier.create(paymentService.checkAvailability(5000))
                .expectNext(PaymentAvailability.enoughFunds(5000))
                .verifyComplete();
    }

    @Test
    void checkAvailability_insufficientFunds_forbidsPurchaseWithMessage() {
        when(paymentsApi.getBalance()).thenReturn(Mono.just(new Balance(4999L)));

        StepVerifier.create(paymentService.checkAvailability(5000))
                .assertNext(availability -> {
                    assertThat(availability.available()).isFalse();
                    assertThat(availability.balance()).isEqualTo(4999L);
                    assertThat(availability.message()).contains("5000", "4999");
                })
                .verifyComplete();
    }

    @Test
    void checkAvailability_serviceDown_forbidsPurchaseWithMessage() {
        when(paymentsApi.getBalance()).thenReturn(Mono.error(connectionRefused()));

        StepVerifier.create(paymentService.checkAvailability(100))
                .expectNext(PaymentAvailability.serviceUnavailable())
                .verifyComplete();
    }

    @Test
    void getBalance_timeout_isReportedAsUnavailable() {
        when(paymentsApi.getBalance()).thenReturn(Mono.never());

        StepVerifier.create(paymentService.getBalance())
                .expectError(PaymentUnavailableException.class)
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void pay_sendsAmountAndReturnsRemainingBalance() {
        when(paymentsApi.pay(any())).thenReturn(Mono.just(new PaymentResult(300L, 700L)));

        StepVerifier.create(paymentService.pay(300))
                .expectNext(700L)
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Mono<PaymentRequest>> captor = ArgumentCaptor.forClass(Mono.class);
        verify(paymentsApi).pay(captor.capture());
        StepVerifier.create(captor.getValue().map(PaymentRequest::getAmount))
                .expectNext(300L)
                .verifyComplete();
    }

    @Test
    void pay_conflict_isReportedAsRejected() {
        when(paymentsApi.pay(any())).thenReturn(Mono.error(WebClientResponseException.create(409, "Conflict",
                HttpHeaders.EMPTY, "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        StepVerifier.create(paymentService.pay(300))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(PaymentRejectedException.class)
                        .hasMessageStartingWith("Оплата не прошла"))
                .verify();
    }

    @Test
    void pay_serverErrorOrConnectionFailure_isReportedAsUnavailable() {
        when(paymentsApi.pay(any())).thenReturn(Mono.error(WebClientResponseException.create(500, "Error",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)));

        StepVerifier.create(paymentService.pay(300))
                .expectError(PaymentUnavailableException.class)
                .verify();

        when(paymentsApi.pay(any())).thenReturn(Mono.error(connectionRefused()));

        StepVerifier.create(paymentService.pay(300))
                .expectError(PaymentUnavailableException.class)
                .verify();
    }

    private static WebClientRequestException connectionRefused() {
        return new WebClientRequestException(new IOException("Connection refused"),
                HttpMethod.GET, URI.create("http://localhost/api/balance"), HttpHeaders.EMPTY);
    }
}
