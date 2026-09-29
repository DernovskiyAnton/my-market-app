package ru.yandex.practicum.mymarket.payment;

import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;
import ru.yandex.practicum.mymarket.support.PaymentServerStub;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class PaymentIntegrationTest extends IntegrationTestBase {

    private long ballId;

    @BeforeEach
    void setUp() {
        ballId = createItem("Оплачиваемый мяч", 1500).getId();
        addToCart(ballId);
        addToCart(ballId);
        PAYMENT_SERVER.reset(DEFAULT_BALANCE);
        forgetPaymentServiceToken();
    }

    @Test
    void cartPage_requestsBalanceOfCurrentUserWithBearerToken() {
        PAYMENT_SERVER.setBalance("alice", 3000);

        assertThat(getHtml("/cart/items")).contains("Итого: 3000 руб.", "Баланс: 3000 руб.", "Купить");

        assertThat(PAYMENT_SERVER.apiRequests())
                .extracting(RecordedRequest::getMethod, RecordedRequest::getPath)
                .containsExactly(tuple("GET", "/api/accounts/alice/balance"));
        RecordedRequest balanceRequest = PAYMENT_SERVER.apiRequests().get(0);
        assertThat(balanceRequest.getHeader("Authorization")).isEqualTo("Bearer " + PAYMENT_SERVER.lastIssuedToken());
        assertThat(balanceRequest.getHeader("Accept")).contains("application/json");
    }

    @Test
    void tokenRequest_usesClientCredentialsGrantWithBasicAuthentication() {
        getHtml("/cart/items");

        assertThat(PAYMENT_SERVER.tokenRequests()).hasSize(1);
        RecordedRequest tokenRequest = PAYMENT_SERVER.tokenRequests().get(0);
        String credentials = Base64.getEncoder().encodeToString(
                (PaymentServerStub.CLIENT_ID + ":" + PaymentServerStub.CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        assertThat(tokenRequest.getMethod()).isEqualTo("POST");
        assertThat(tokenRequest.getHeader("Authorization")).isEqualTo("Basic " + credentials);
        assertThat(tokenRequest.getBody().clone().readUtf8())
                .contains("grant_type=client_credentials", "scope=payments");
    }

    @Test
    void token_isReusedUntilPaymentServiceRejectsIt() {
        getHtml("/cart/items");
        getHtml("/cart/items");
        assertThat(PAYMENT_SERVER.tokenRequests()).hasSize(1);
        String firstToken = PAYMENT_SERVER.lastIssuedToken();

        PAYMENT_SERVER.revokeIssuedTokens();
        assertThat(getHtml("/cart/items")).contains(PaymentClientErrorException.MESSAGE);
        assertThat(getHtml("/cart/items")).contains("Купить");

        assertThat(PAYMENT_SERVER.tokenRequests()).hasSize(2);
        assertThat(PAYMENT_SERVER.apiRequests().get(PAYMENT_SERVER.apiRequests().size() - 1).getHeader("Authorization"))
                .isEqualTo("Bearer " + PAYMENT_SERVER.lastIssuedToken())
                .isNotEqualTo("Bearer " + firstToken);
    }

    @Test
    void clientNotAuthorizedOnAuthorizationServer_cannotCallPaymentService() {
        PAYMENT_SERVER.rejectClientCredentials();

        assertThat(getHtml("/cart/items")).contains(PaymentClientErrorException.MESSAGE)
                .doesNotContain("Купить", PaymentUnavailableException.MESSAGE);
        asAlice().post().uri("/buy").exchange()
                .expectStatus().isEqualTo(502)
                .expectBody(String.class).value(html -> assertThat(html).contains("не авторизована"));

        assertThat(PAYMENT_SERVER.apiRequests()).isEmpty();
        assertThat(countOrders()).isZero();
    }

    @Test
    void authorizationServerUnavailable_isReportedAsPaymentServiceUnavailable() {
        PAYMENT_SERVER.respondWith(PaymentServerStub.TOKEN_PATH, 500, "{}");

        assertThat(getHtml("/cart/items")).contains(PaymentUnavailableException.MESSAGE)
                .doesNotContain("Купить", PaymentClientErrorException.MESSAGE);
    }

    @Test
    void cartPage_insufficientFunds_hidesBuyButton() {
        PAYMENT_SERVER.setBalance("alice", 2999);

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
    void buy_paysFromCurrentUsersAccountAndCreatesOrder() {
        PAYMENT_SERVER.setBalance("alice", 5000);

        String orderUrl = asAlice().post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectBody().returnResult().getResponseHeaders().getLocation().toString();

        assertThat(PAYMENT_SERVER.paymentBodies()).containsExactly("{\"amount\":3000}");
        RecordedRequest payment = PAYMENT_SERVER.apiRequests().get(0);
        assertThat(payment.getMethod()).isEqualTo("POST");
        assertThat(payment.getPath()).isEqualTo("/api/accounts/alice/payments");
        assertThat(payment.getHeader("Authorization")).startsWith("Bearer test-access-token-");
        assertThat(payment.getHeader("Content-Type")).contains("application/json");
        assertThat(PAYMENT_SERVER.balance("alice")).isEqualTo(2000);
        assertThat(PAYMENT_SERVER.balance("bob")).isEqualTo(DEFAULT_BALANCE);
        assertThat(getHtml(orderUrl)).contains("Успешная покупка", "Оплачиваемый мяч", "Сумма: 3000 руб.");
        assertThat(getHtml("/cart/items")).doesNotContain("Оплачиваемый мяч");
    }

    @Test
    void buy_paymentRequestIsSentWithoutOpenDatabaseTransaction() {
        AtomicLong uncommittedSessionsDuringPayment = new AtomicLong(-1);
        AtomicLong ordersDuringPayment = new AtomicLong(-1);
        PAYMENT_SERVER.onPayment(() -> {
            uncommittedSessionsDuringPayment.set(countSessionsWithUncommittedChanges());
            ordersDuringPayment.set(countOrders());
        });

        asAlice().post().uri("/buy").exchange().expectStatus().is3xxRedirection();

        assertThat(uncommittedSessionsDuringPayment).hasValue(0);
        assertThat(ordersDuringPayment).hasValue(0);
        assertThat(countOrders()).isEqualTo(1);
    }

    @Test
    void buy_insufficientFunds_createsNoOrderAndKeepsCart() {
        PAYMENT_SERVER.setBalance("alice", 100);

        asAlice().post().uri("/buy").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Оплата не прошла: Недостаточно средств на балансе"));

        assertThat(PAYMENT_SERVER.balance("alice")).isEqualTo(100);
        assertThat(countOrders()).isZero();
        PAYMENT_SERVER.setBalance("alice", 100_000);
        assertThat(getHtml("/cart/items")).contains("Оплачиваемый мяч", "Итого: 3000 руб.");
    }

    @Test
    void cartPage_balanceRequestRejected_showsRequestRejectedMessageNotUnavailable() {
        PAYMENT_SERVER.respondWith("/api/accounts/alice/balance", 404,
                "{\"code\":\"INVALID_REQUEST\",\"message\":\"Нет такого ресурса\"}");

        assertThat(getHtml("/cart/items")).contains(PaymentClientErrorException.MESSAGE)
                .doesNotContain("Купить", PaymentUnavailableException.MESSAGE);
    }

    @Test
    void buy_paymentRequestRejectedAsInvalid_isReportedAsClientErrorNotAsUnavailable() {
        PAYMENT_SERVER.respondWith("/api/accounts/alice/payments", 400,
                "{\"code\":\"INVALID_REQUEST\",\"message\":\"Некорректный запрос: сумма должна быть положительной\"}");

        asAlice().post().uri("/buy").exchange()
                .expectStatus().isEqualTo(502)
                .expectBody(String.class)
                .value(html -> assertThat(html)
                        .contains(PaymentClientErrorException.MESSAGE, "Некорректный запрос: сумма должна быть положительной")
                        .doesNotContain(PaymentUnavailableException.MESSAGE));

        assertThat(countOrders()).isZero();
        assertThat(getHtml("/cart/items")).contains("Оплачиваемый мяч");
    }

    @Test
    void buy_paymentServiceUnavailable_createsNoOrderAndKeepsCart() {
        PAYMENT_SERVER.makeUnavailable();

        asAlice().post().uri("/buy").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class).value(html -> assertThat(html).contains(PaymentUnavailableException.MESSAGE));

        assertThat(countOrders()).isZero();
        PAYMENT_SERVER.reset(DEFAULT_BALANCE);
        assertThat(getHtml("/cart/items")).contains("Оплачиваемый мяч");
    }

    private long countOrders() {
        return databaseClient.sql("SELECT COUNT(*) AS cnt FROM orders")
                .map(row -> row.get("cnt", Long.class))
                .one()
                .block();
    }

    private long countSessionsWithUncommittedChanges() {
        return databaseClient.sql("SELECT COUNT(*) AS cnt FROM INFORMATION_SCHEMA.SESSIONS WHERE CONTAINS_UNCOMMITTED")
                .map(row -> row.get("cnt", Long.class))
                .one()
                .block();
    }

    private void addToCart(long itemId) {
        asAlice().post().uri("/cart/items?id=" + itemId + "&action=PLUS").exchange().expectStatus().isOk();
    }

    private String getHtml(String uri) {
        return asAlice().get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
