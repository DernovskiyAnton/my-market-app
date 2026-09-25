package ru.yandex.practicum.payment.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.account.AccountService;
import ru.yandex.practicum.payment.account.InsufficientFundsException;
import ru.yandex.practicum.payment.api.PaymentsApiController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PaymentsApiController.class)
@Import(PaymentsApiDelegateImpl.class)
class PaymentsApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AccountService accountService;

    @Test
    void getBalance_returnsBalanceAsJson() {
        when(accountService.getBalance()).thenReturn(Mono.just(50000L));

        webTestClient.get().uri("/api/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json("{\"amount\":50000}", true);
    }

    @Test
    void pay_withdrawsAmountAndReturnsRemainingBalance() {
        when(accountService.withdraw(5600)).thenReturn(Mono.just(44400L));

        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":5600}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{\"amount\":5600,\"balance\":44400}", true);
    }

    @Test
    void pay_insufficientFunds_returnsConflict() {
        when(accountService.withdraw(999999)).thenReturn(Mono.error(new InsufficientFundsException(999999, 100)));

        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":999999}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS")
                .jsonPath("$.message").value(message -> assertThat((String) message).contains("999999"));
    }

    @Test
    void pay_invalidAmount_returnsBadRequestWithoutWithdrawal() {
        for (String body : new String[]{"{\"amount\":0}", "{\"amount\":-1}", "{}", "not json"}) {
            webTestClient.post().uri("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                    .jsonPath("$.message").isEqualTo(ApiExceptionHandler.INVALID_REQUEST_MESSAGE);
        }

        verify(accountService, never()).withdraw(anyLong());
    }
}
