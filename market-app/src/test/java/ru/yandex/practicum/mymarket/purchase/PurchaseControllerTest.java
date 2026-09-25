package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.payment.PaymentRejectedException;
import ru.yandex.practicum.mymarket.payment.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PurchaseControllerTest extends ControllerTestBase {

    @Test
    void buy_redirectsToNewOrder() {
        when(purchaseService.buy()).thenReturn(Mono.just(7L));

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/orders/7?newOrder=true");
    }

    @Test
    void buy_emptyCart_rendersBadRequestPage() {
        when(purchaseService.buy()).thenReturn(Mono.error(new EmptyCartException()));

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Невозможно оформить заказ: корзина пуста"));
    }

    @Test
    void buy_paymentRejected_rendersConflictPage() {
        when(purchaseService.buy()).thenReturn(Mono.error(new PaymentRejectedException("Оплата не прошла: мало денег")));

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class).value(html -> assertThat(html).contains("Оплата не прошла: мало денег"));
    }

    @Test
    void buy_paymentServiceUnavailable_rendersServiceUnavailablePage() {
        when(purchaseService.buy())
                .thenReturn(Mono.error(new PaymentUnavailableException(new RuntimeException("Connection refused"))));

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class).value(html -> assertThat(html).contains(PaymentUnavailableException.MESSAGE));
    }
}
