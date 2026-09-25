package ru.yandex.practicum.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.payment.api.model.Balance;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment.api.model.PaymentResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "payment.account.initial-balance=10000")
class PaymentServiceIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void pay_withdrawsAmountFromBalance() {
        long before = currentBalance();

        PaymentResult result = webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest(100L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResult.class).returnResult().getResponseBody();

        assertThat(result.getAmount()).isEqualTo(100L);
        assertThat(result.getBalance()).isEqualTo(before - 100);
        assertThat(currentBalance()).isEqualTo(before - 100);
    }

    @Test
    void pay_moreThanBalance_isRejectedAndBalanceUnchanged() {
        long before = currentBalance();

        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest(before + 1))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS");

        assertThat(currentBalance()).isEqualTo(before);
    }

    @Test
    void pay_invalidRequest_isRejected() {
        webTestClient.post().uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":0}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    private long currentBalance() {
        Balance balance = webTestClient.get().uri("/api/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Balance.class).returnResult().getResponseBody();
        assertThat(balance.getAmount()).isBetween(0L, 10000L);
        return balance.getAmount();
    }
}
