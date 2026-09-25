package ru.yandex.practicum.mymarket.order;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OrderControllerTest extends ControllerTestBase {

    private static final OrderDto ORDER = new OrderDto(7L,
            List.of(new OrderItemDto(1L, "Мяч", 2500, 2), new OrderItemDto(2L, "Скакалка", 600, 1)), 5600);

    @Test
    void getOrders_rendersOrderList() {
        when(orderService.findAll()).thenReturn(Flux.just(ORDER));

        webTestClient.get().uri("/orders").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Заказ №7", "href=\"/orders/7\"",
                        "Мяч (2 шт.) 5000 руб.", "Скакалка (1 шт.) 600 руб.", "Сумма: 5600 руб."));
    }

    @Test
    void getOrder_newOrder_showsSuccessAlert() {
        when(orderService.getOrder(7L)).thenReturn(Mono.just(ORDER));

        webTestClient.get().uri("/orders/7?newOrder=true").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Заказ №7", "Успешная покупка", "Сумма: 5600 руб."));
    }

    @Test
    void getOrder_byDefault_isNotNewOrder() {
        when(orderService.getOrder(7L)).thenReturn(Mono.just(ORDER));

        webTestClient.get().uri("/orders/7").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).doesNotContain("Успешная покупка"));
    }

    @Test
    void getOrder_unknownId_returnsNotFound() {
        when(orderService.getOrder(99L)).thenReturn(Mono.error(NotFoundException.order(99L)));

        webTestClient.get().uri("/orders/99").exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(html -> assertThat(html).contains("Заказ с id=99 не найден"));
    }
}
