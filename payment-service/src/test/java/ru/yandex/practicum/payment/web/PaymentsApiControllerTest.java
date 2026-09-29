package ru.yandex.practicum.payment.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.account.AccountService;
import ru.yandex.practicum.payment.account.InsufficientFundsException;
import ru.yandex.practicum.payment.api.PaymentsApiController;
import ru.yandex.practicum.payment.security.SecurityConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = PaymentsApiController.class)
@Import({PaymentsApiDelegateImpl.class, SecurityConfig.class})
class PaymentsApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AccountService accountService;

    @Test
    void getBalance_returnsBalanceOfRequestedAccount() {
        when(accountService.getBalance("alice")).thenReturn(Mono.just(50000L));

        authorized().get().uri("/api/accounts/alice/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json("{\"amount\":50000}", true);
    }

    @Test
    void pay_withdrawsAmountFromRequestedAccount() {
        when(accountService.withdraw("alice", 5600)).thenReturn(Mono.just(44400L));

        authorized().post().uri("/api/accounts/alice/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":5600}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{\"amount\":5600,\"balance\":44400}", true);
    }

    @Test
    void pay_insufficientFunds_returnsConflict() {
        when(accountService.withdraw("bob", 999999)).thenReturn(Mono.error(new InsufficientFundsException(999999, 100)));

        authorized().post().uri("/api/accounts/bob/payments")
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
            authorized().post().uri("/api/accounts/alice/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                    .jsonPath("$.message").isEqualTo(ApiExceptionHandler.INVALID_REQUEST_MESSAGE);
        }

        verify(accountService, never()).withdraw(anyString(), anyLong());
    }

    @Test
    void invalidUsername_returnsBadRequest() {
        authorized().get().uri("/api/accounts/{username}/balance", "bad name!")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_REQUEST");

        verifyNoInteractions(accountService);
    }

    @Test
    void requestWithoutToken_isUnauthorized() {
        webTestClient.get().uri("/api/accounts/alice/balance")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches("WWW-Authenticate", "Bearer.*");
        webTestClient.post().uri("/api/accounts/alice/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":1}")
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(accountService);
    }

    @Test
    void tokenWithoutPaymentsScope_isForbidden() {
        webTestClient.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_profile")))
                .get().uri("/api/accounts/alice/balance")
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(accountService);
    }

    @Test
    void unknownPath_isDeniedEvenWithValidToken() {
        authorized().get().uri("/internal/state")
                .exchange()
                .expectStatus().isForbidden();
    }

    private WebTestClient authorized() {
        return webTestClient.mutateWith(mockJwt()
                .jwt(jwt -> jwt.subject("market-app").audience(List.of("payment-service")))
                .authorities(new SimpleGrantedAuthority(SecurityConfig.PAYMENTS_AUTHORITY)));
    }
}
