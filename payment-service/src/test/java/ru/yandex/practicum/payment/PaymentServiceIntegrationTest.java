package ru.yandex.practicum.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.payment.api.model.Balance;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment.api.model.PaymentResult;
import ru.yandex.practicum.payment.support.TestTokens;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"payment.account.initial-balance=10000", "payment.account.balances.poor=100"})
class PaymentServiceIntegrationTest {

    @DynamicPropertySource
    static void jwkSetUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", TestTokens::jwkSetUri);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void pay_withValidToken_withdrawsAmountFromUsersAccountOnly() {
        long aliceBefore = balanceOf("alice");
        long carolBefore = balanceOf("carol");

        PaymentResult result = webTestClient.post().uri("/api/accounts/alice/payments")
                .headers(headers -> headers.setBearerAuth(TestTokens.valid()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest(100L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResult.class).returnResult().getResponseBody();

        assertThat(result.getAmount()).isEqualTo(100L);
        assertThat(result.getBalance()).isEqualTo(aliceBefore - 100);
        assertThat(balanceOf("alice")).isEqualTo(aliceBefore - 100);
        assertThat(balanceOf("carol")).isEqualTo(carolBefore);
    }

    @Test
    void pay_moreThanBalance_isRejectedAndBalanceUnchanged() {
        webTestClient.post().uri("/api/accounts/poor/payments")
                .headers(headers -> headers.setBearerAuth(TestTokens.valid()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest(101L))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS");

        assertThat(balanceOf("poor")).isEqualTo(100L);
    }

    @Test
    void requestWithoutToken_isUnauthorized() {
        webTestClient.get().uri("/api/accounts/alice/balance")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokensThatFailValidation_areUnauthorized() {
        String[] invalidTokens = {
                TestTokens.builder().signedWithForeignKey().sign(),
                TestTokens.builder().expiresAt(Instant.now().minus(Duration.ofMinutes(10))).sign(),
                TestTokens.builder().issuer("http://evil.example").sign(),
                TestTokens.builder().audience("other-service").sign(),
                "not-a-jwt"
        };
        for (String token : invalidTokens) {
            webTestClient.get().uri("/api/accounts/alice/balance")
                    .headers(headers -> headers.setBearerAuth(token))
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().valueMatches("WWW-Authenticate", "Bearer.*invalid_token.*");
        }
    }

    @Test
    void tokenWithoutPaymentsScope_isForbidden() {
        webTestClient.post().uri("/api/accounts/alice/payments")
                .headers(headers -> headers.setBearerAuth(TestTokens.builder().scopes("profile").sign()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest(1L))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void healthEndpoint_isAvailableWithoutToken() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    private long balanceOf(String username) {
        Balance balance = webTestClient.get().uri("/api/accounts/{username}/balance", username)
                .headers(headers -> headers.setBearerAuth(TestTokens.valid()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Balance.class).returnResult().getResponseBody();
        return balance.getAmount();
    }
}
