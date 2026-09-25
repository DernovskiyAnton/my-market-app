package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
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
}
