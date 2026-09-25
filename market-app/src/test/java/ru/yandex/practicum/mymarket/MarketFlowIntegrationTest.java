package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class MarketFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void catalog_searchSortAndPaging() {
        createItem("Зелёный тестовый товар", 100);
        createItem("Алый тестовый товар", 200);
        createItem("Белый тестовый товар", 300);
        createItem("Бежевый тестовый товар", 400);

        String byPrice = getHtml("/items?search=тестовый&sort=PRICE&pageSize=5");
        assertThat(byPrice).containsSubsequence("Зелёный тестовый товар", "Алый тестовый товар",
                "Белый тестовый товар", "Бежевый тестовый товар");
        assertThat(byPrice).contains("Страница: 1").doesNotContain("value=\"2\" form=\"main\"");

        String secondPageByTitle = getHtml("/items?search=тестовый&sort=ALPHA&pageSize=2&pageNumber=2");
        assertThat(secondPageByTitle).containsSubsequence("Белый тестовый товар", "Зелёный тестовый товар");
        assertThat(secondPageByTitle).doesNotContain("Алый тестовый товар", "Бежевый тестовый товар");
        assertThat(secondPageByTitle).contains("Страница: 2", "value=\"1\" form=\"main\"")
                .doesNotContain("value=\"3\" form=\"main\"");
    }

    @Test
    void purchaseFlow_fromCatalogToOrder() {
        long ballId = createItem("Тестовый мяч", 1000).getId();
        long ropeId = createItem("Тестовая скакалка", 300).getId();

        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", String.valueOf(ballId)).with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items?search=&sort=NO&pageNumber=1&pageSize=5");
        webTestClient.post().uri("/items/" + ballId)
                .body(BodyInserters.fromFormData("action", "PLUS"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("<span>2</span>"));
        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", String.valueOf(ropeId)).with("action", "PLUS"))
                .exchange()
                .expectStatus().isOk();

        assertThat(getHtml("/cart/items")).contains("Тестовый мяч", "Тестовая скакалка", "Итого: 2300 руб.");

        URI orderUri = webTestClient.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectBody().returnResult().getResponseHeaders().getLocation();
        assertThat(orderUri.toString()).matches("/orders/\\d+\\?newOrder=true");

        assertThat(getHtml(orderUri.toString())).contains("Успешная покупка", "Сумма: 2300 руб.");
        assertThat(getHtml("/cart/items")).doesNotContain("Тестовый мяч", "Купить");
        assertThat(getHtml("/orders")).contains("Тестовый мяч (2 шт.) 2000 руб.");
    }

    @Test
    void buy_withEmptyCart_returnsBadRequest() {
        webTestClient.post().uri("/buy").exchange().expectStatus().isBadRequest();
    }

    @Test
    void unknownItemAndOrder_returnNotFound() {
        webTestClient.get().uri("/items/100500").exchange().expectStatus().isNotFound();
        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "100500").with("action", "PLUS"))
                .exchange()
                .expectStatus().isNotFound();
        webTestClient.get().uri("/orders/100500").exchange().expectStatus().isNotFound();
    }

    @Test
    void bundledImage_isServed() {
        webTestClient.get().uri("/images/ball.svg").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("image/svg+xml");
    }

    private String getHtml(String uri) {
        return webTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
