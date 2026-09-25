package ru.yandex.practicum.mymarket.payment;

import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class PaymentIntegrationTest extends IntegrationTestBase {

    private long ballId;

    @BeforeEach
    void setUp() {
        ballId = createItem("Оплачиваемый мяч", 1500).getId();
        addToCart(ballId);
        addToCart(ballId);
    }

    @Test
    void cartPage_requestsBalanceAndShowsBuyButtonWhenFundsAreEnough() {
        PAYMENT_SERVER.reset(3000);

        assertThat(getHtml("/cart/items")).contains("Итого: 3000 руб.", "Баланс: 3000 руб.", "Купить");

        assertThat(PAYMENT_SERVER.requests())
                .extracting(RecordedRequest::getMethod, RecordedRequest::getPath)
                .containsExactly(tuple("GET", "/api/balance"));
        assertThat(PAYMENT_SERVER.requests().get(0).getHeader("Accept")).contains("application/json");
    }

    @Test
    void cartPage_insufficientFunds_hidesBuyButton() {
        PAYMENT_SERVER.reset(2999);

        assertThat(getHtml("/cart/items"))
                .contains("Недостаточно средств на балансе для оплаты заказа: нужно 3000 руб., доступно 2999 руб.")
                .doesNotContain("Купить");
    }

    @Test
    void cartPage_paymentServiceUnavailable_hidesBuyButton() {
        PAYMENT_SERVER.makeUnavailable();

        assertThat(getHtml("/cart/items")).contains(PaymentUnavailableException.MESSAGE).doesNotContain("Купить");
    }

    @Test
    void buy_sendsPaymentRequestAndCreatesOrder() {
        PAYMENT_SERVER.reset(5000);

        String orderUrl = webTestClient.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectBody().returnResult().getResponseHeaders().getLocation().toString();

        assertThat(PAYMENT_SERVER.paymentBodies()).containsExactly("{\"amount\":3000}");
        RecordedRequest payment = PAYMENT_SERVER.requests().get(0);
        assertThat(payment.getMethod()).isEqualTo("POST");
        assertThat(payment.getHeader("Content-Type")).contains("application/json");
        assertThat(PAYMENT_SERVER.balance()).isEqualTo(2000);
        assertThat(getHtml(orderUrl)).contains("Успешная покупка", "Оплачиваемый мяч", "Сумма: 3000 руб.");
        assertThat(getHtml("/cart/items")).doesNotContain("Оплачиваемый мяч");
    }

    @Test
    void buy_insufficientFunds_rollsBackOrderAndKeepsCart() {
        PAYMENT_SERVER.reset(100);

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Оплата не прошла: Недостаточно средств на балансе"));

        assertThat(PAYMENT_SERVER.balance()).isEqualTo(100);
        assertThat(countOrders()).isZero();
        assertThat(getHtmlWithBalance(100_000, "/cart/items")).contains("Оплачиваемый мяч", "Итого: 3000 руб.");
    }

    @Test
    void buy_paymentServiceUnavailable_rollsBackOrderAndKeepsCart() {
        PAYMENT_SERVER.makeUnavailable();

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class).value(html -> assertThat(html).contains(PaymentUnavailableException.MESSAGE));

        assertThat(countOrders()).isZero();
        assertThat(getHtmlWithBalance(100_000, "/cart/items")).contains("Оплачиваемый мяч");
    }

    private long countOrders() {
        return databaseClient.sql("SELECT COUNT(*) AS cnt FROM orders")
                .map(row -> row.get("cnt", Long.class))
                .one()
                .block();
    }

    private void addToCart(long itemId) {
        webTestClient.post().uri("/cart/items?id=" + itemId + "&action=PLUS").exchange().expectStatus().isOk();
    }

    private String getHtmlWithBalance(long balance, String uri) {
        PAYMENT_SERVER.reset(balance);
        return getHtml(uri);
    }

    private String getHtml(String uri) {
        return webTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
