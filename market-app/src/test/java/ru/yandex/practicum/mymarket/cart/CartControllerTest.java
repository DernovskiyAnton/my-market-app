package ru.yandex.practicum.mymarket.cart;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.payment.PaymentAvailability;
import ru.yandex.practicum.mymarket.payment.PaymentUnavailableException;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CartControllerTest extends ControllerTestBase {

    private static final ItemDto BALL = new ItemDto(1L, "Мяч", "Футбольный мяч", "images/ball.svg", 2500, 2);

    @Test
    void getCart_enoughFunds_rendersItemsTotalAndBuyButton() {
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(BALL), 5000)));
        when(paymentService.checkAvailability(5000)).thenReturn(Mono.just(PaymentAvailability.enoughFunds(8000)));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Футбольный мяч", "<span>2</span>", "Итого: 5000 руб.",
                        "Баланс: 8000 руб.", "Купить").doesNotContain("alert-warning"));
    }

    @Test
    void getCart_insufficientFunds_hidesBuyButtonAndShowsMessage() {
        PaymentAvailability availability = PaymentAvailability.insufficientFunds(1000, 5000);
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(BALL), 5000)));
        when(paymentService.checkAvailability(5000)).thenReturn(Mono.just(availability));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains(availability.message(), "Баланс: 1000 руб.")
                        .doesNotContain("Купить"));
    }

    @Test
    void getCart_paymentServiceUnavailable_hidesBuyButtonAndShowsMessage() {
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(BALL), 5000)));
        when(paymentService.checkAvailability(5000)).thenReturn(Mono.just(PaymentAvailability.serviceUnavailable()));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains(PaymentUnavailableException.MESSAGE)
                        .doesNotContain("Купить", "Баланс:"));
    }

    @Test
    void getCart_empty_hidesBuyButtonWithoutCallingPaymentService() {
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(), 0)));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).doesNotContain("Купить"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void changeCartItem_appliesActionAndRendersCart() {
        when(cartService.changeQuantity(1L, CartAction.DELETE)).thenReturn(Mono.empty());
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(), 0)));

        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).doesNotContain("Мяч"));

        verify(cartService).changeQuantity(1L, CartAction.DELETE);
    }

    @Test
    void changeCartItem_unknownItem_returnsNotFound() {
        when(cartService.changeQuantity(99L, CartAction.PLUS)).thenReturn(Mono.error(NotFoundException.item(99L)));

        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "99").with("action", "PLUS"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void changeCartItem_invalidParams_returnsBadRequest() {
        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().isBadRequest();
        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "abc").with("action", "PLUS"))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService, paymentService);
    }
}
