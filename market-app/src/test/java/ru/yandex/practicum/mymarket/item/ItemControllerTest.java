package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.common.GlobalExceptionHandler;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemControllerTest extends ControllerTestBase {

    private static final ItemDto BALL = new ItemDto(1L, "Мяч", "Футбольный мяч", "images/ball.svg", 2500, 2);
    private static final ItemDto ROPE = new ItemDto(2L, "Скакалка", "Скоростная", "images/rope.svg", 600, 0);

    @Test
    void getItems_withDefaults_rendersFirstPage() {
        when(itemService.findItems("", SortType.NO, 1, 5))
                .thenReturn(Mono.just(new PageImpl<>(List.of(BALL), PageRequest.of(0, 5), 1)));

        String html = getHtml("/");

        assertThat(html).contains("Футбольный мяч", "2500 руб.", "Страница: 1", "href=\"/items/1\"");
        assertThat(html).doesNotContain("&larr;", "&rarr;", "←", "→");
    }

    @Test
    void getItems_withParams_passesThemToServiceAndRendersPaging() {
        when(itemService.findItems("мяч", SortType.PRICE, 2, 2))
                .thenReturn(Mono.just(new PageImpl<>(List.of(BALL, ROPE), PageRequest.of(1, 2), 6)));

        String html = getHtml("/items?search=мяч&sort=PRICE&pageNumber=2&pageSize=2");

        assertThat(html).contains("Мяч", "Скакалка", "Страница: 2", "value=\"мяч\"");
        assertThat(html).containsPattern("<option value=\"PRICE\" selected=\"selected\">");
        assertThat(html).containsPattern("<option value=\"2\" selected=\"selected\">");
        assertThat(html).contains("value=\"1\" form=\"main\"", "value=\"3\" form=\"main\"");
    }

    @Test
    void getItems_invalidPaging_returnsBadRequest() {
        webTestClient.get().uri("/items?pageSize=0").exchange().expectStatus().isBadRequest();
        webTestClient.get().uri("/items?pageSize=101").exchange().expectStatus().isBadRequest();
        webTestClient.get().uri("/items?pageNumber=0").exchange().expectStatus().isBadRequest();

        verifyNoInteractions(itemService);
    }

    @Test
    void getItems_unknownSortOrNonNumericPageSize_returnsBadRequest() {
        webTestClient.get().uri("/items?sort=RANDOM").exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).value(html -> assertThat(html).contains(GlobalExceptionHandler.BAD_REQUEST_MESSAGE));
        webTestClient.get().uri("/items?pageSize=abc").exchange()
                .expectStatus().isBadRequest();

        verify(itemService, never()).findItems(anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void changeCartItemFromCatalog_redirectsBackWithParams() {
        when(cartService.changeQuantity(1L, CartAction.PLUS)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "мяч")
                        .with("sort", "ALPHA")
                        .with("pageNumber", "3")
                        .with("pageSize", "10"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=%D0%BC%D1%8F%D1%87&sort=ALPHA&pageNumber=3&pageSize=10");

        verify(cartService).changeQuantity(1L, CartAction.PLUS);
    }

    @Test
    void changeCartItemFromCatalog_withoutOptionalParams_usesDefaults() {
        when(cartService.changeQuantity(1L, CartAction.MINUS)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items?id=1&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=&sort=NO&pageNumber=1&pageSize=5");
    }

    @Test
    void changeCartItemFromCatalog_withoutAction_returnsBadRequest() {
        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1"))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService);
    }

    @Test
    void getItem_rendersItemPage() {
        when(itemService.getItem(1L)).thenReturn(Mono.just(BALL));

        assertThat(getHtml("/items/1")).contains("Футбольный мяч", "2500 руб.", "<span>2</span>",
                "action=\"/items/1\"");
    }

    @Test
    void getItem_unknownId_rendersNotFoundPage() {
        when(itemService.getItem(99L)).thenReturn(Mono.error(NotFoundException.item(99L)));

        webTestClient.get().uri("/items/99").exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(html -> assertThat(html).contains("404", "Товар с id=99 не найден"));
    }

    @Test
    void changeCartItemFromItemPage_rendersUpdatedItem() {
        when(cartService.changeQuantity(1L, CartAction.MINUS)).thenReturn(Mono.empty());
        when(itemService.getItem(1L)).thenReturn(Mono.just(BALL));

        webTestClient.post().uri("/items/1")
                .body(BodyInserters.fromFormData("action", "MINUS"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Футбольный мяч", "<span>2</span>"));

        verify(cartService).changeQuantity(1L, CartAction.MINUS);
    }

    @Test
    void changeCartItemFromItemPage_unknownId_returnsNotFound() {
        when(cartService.changeQuantity(99L, CartAction.PLUS)).thenReturn(Mono.error(NotFoundException.item(99L)));

        webTestClient.post().uri("/items/99")
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().isNotFound();
    }

    private String getHtml(String uri) {
        return webTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
