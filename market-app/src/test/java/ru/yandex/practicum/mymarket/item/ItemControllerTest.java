package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

class ItemControllerTest extends ControllerTestBase {

    private static final ItemDto BALL = new ItemDto(1L, "Мяч", "Футбольный мяч", "images/ball.svg", 2500, 2);
    private static final ItemDto ROPE = new ItemDto(2L, "Скакалка", "Скоростная", "images/rope.svg", 600, 0);

    @Test
    void getItems_withDefaults_rendersFirstPage() {
        when(itemService.findItems(null, "", SortType.NO, 1, 5))
                .thenReturn(Mono.just(new PageImpl<>(List.of(BALL), PageRequest.of(0, 5), 1)));

        String html = getHtml("/");

        assertThat(html).contains("Футбольный мяч", "2500 руб.", "Страница: 1", "href=\"/items/1\"");
        assertThat(html).doesNotContain("&larr;", "&rarr;", "←", "→");
    }

    @Test
    void getItems_anonymous_hidesCartControlsAndPrivateLinks() {
        when(itemService.findItems(null, "", SortType.NO, 1, 5))
                .thenReturn(Mono.just(new PageImpl<>(List.of(BALL), PageRequest.of(0, 5), 1)));

        String html = getHtml("/items");

        assertThat(html).contains("Войдите, чтобы купить", "href=\"/login\"");
        assertThat(html).doesNotContain("value=\"PLUS\"", "value=\"MINUS\"", "href=\"/cart/items\"",
                "href=\"/orders\"", "href=\"/admin/items\"", "Выйти");
    }

    @Test
    void getItems_authenticated_showsCartControlsForCurrentUser() {
        when(itemService.findItems(ALICE.getId(), "", SortType.NO, 1, 5))
                .thenReturn(Mono.just(new PageImpl<>(List.of(BALL), PageRequest.of(0, 5), 1)));

        String html = asAlice().get().uri("/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(html).contains("value=\"PLUS\"", "value=\"MINUS\"", "href=\"/cart/items\"", "href=\"/orders\"",
                "Выйти", "alice", "name=\"_csrf\"");
        assertThat(html).doesNotContain("Войдите, чтобы купить", "href=\"/admin/items\"");
    }

    @Test
    void getItems_withParams_passesThemToServiceAndRendersPaging() {
        when(itemService.findItems(null, "мяч", SortType.PRICE, 2, 2))
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

        verify(itemService, never()).findItems(any(), anyString(), any(), anyInt(), anyInt());
    }

    @Test
    void changeCartItemFromCatalog_redirectsBackWithParams() {
        when(cartService.changeQuantity(ALICE.getId(), 1L, CartAction.PLUS)).thenReturn(Mono.empty());

        asAlice().post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "мяч")
                        .with("sort", "ALPHA")
                        .with("pageNumber", "3")
                        .with("pageSize", "10"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=%D0%BC%D1%8F%D1%87&sort=ALPHA&pageNumber=3&pageSize=10");

        verify(cartService).changeQuantity(ALICE.getId(), 1L, CartAction.PLUS);
    }

    @Test
    void changeCartItemFromCatalog_withoutOptionalParams_usesDefaults() {
        when(cartService.changeQuantity(ALICE.getId(), 1L, CartAction.MINUS)).thenReturn(Mono.empty());

        asAlice().post().uri("/items?id=1&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=&sort=NO&pageNumber=1&pageSize=5");
    }

    @Test
    void changeCartItemFromCatalog_withoutAction_returnsBadRequest() {
        asAlice().post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1"))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(cartService);
    }

    @Test
    void getItem_anonymous_rendersItemPageWithoutCartControls() {
        when(itemService.getItem(null, 1L)).thenReturn(Mono.just(BALL));

        assertThat(getHtml("/items/1")).contains("Футбольный мяч", "2500 руб.", "Войдите, чтобы купить")
                .doesNotContain("action=\"/items/1\"", "value=\"PLUS\"");
    }

    @Test
    void getItem_authenticated_rendersCartControlsWithCountInCart() {
        when(itemService.getItem(ALICE.getId(), 1L)).thenReturn(Mono.just(BALL));

        String html = asAlice().get().uri("/items/1").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(html).contains("Футбольный мяч", "<span>2</span>", "action=\"/items/1\"", "name=\"_csrf\"")
                .doesNotContain("Войдите, чтобы купить");
    }

    @Test
    void getItem_unknownId_rendersNotFoundPage() {
        when(itemService.getItem(null, 99L)).thenReturn(Mono.error(NotFoundException.item(99L)));

        webTestClient.get().uri("/items/99").exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(html -> assertThat(html).contains("404", "Товар с id=99 не найден"));
    }

    @Test
    void changeCartItemFromItemPage_rendersUpdatedItem() {
        when(cartService.changeQuantity(ALICE.getId(), 1L, CartAction.MINUS)).thenReturn(Mono.empty());
        when(itemService.getItem(ALICE.getId(), 1L)).thenReturn(Mono.just(BALL));

        asAlice().post().uri("/items/1")
                .body(BodyInserters.fromFormData("action", "MINUS"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Футбольный мяч", "<span>2</span>"));

        verify(cartService).changeQuantity(ALICE.getId(), 1L, CartAction.MINUS);
    }

    @Test
    void changeCartItemFromItemPage_unknownId_returnsNotFound() {
        when(cartService.changeQuantity(ALICE.getId(), 99L, CartAction.PLUS)).thenReturn(Mono.error(NotFoundException.item(99L)));

        asAlice().post().uri("/items/99")
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void changeCartItem_anonymous_isRedirectedToLogin() {
        anonymousWithCsrf().post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login?required");
        anonymousWithCsrf().post().uri("/items/1")
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login?required");

        verifyNoInteractions(cartService);
    }

    @Test
    void changeCartItem_withoutCsrfToken_isForbidden() {
        webTestClient.mutateWith(mockAuthentication(new UsernamePasswordAuthenticationToken(ALICE, null,
                        ALICE.getAuthorities())))
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(cartService);
    }

    private String getHtml(String uri) {
        return webTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
