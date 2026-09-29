package ru.yandex.practicum.mymarket.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LoginIntegrationTest extends IntegrationTestBase {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Test
    void formLogin_withDatabaseUser_opensPrivatePagesUntilLogout() {
        Session session = openLoginPage();

        String authenticatedSession = login(session, "alice", "alice123");
        String cart = page(authenticatedSession, "/cart/items");
        assertThat(cart).contains("alice", "Выйти");

        String csrf = csrf(page(authenticatedSession, "/items"));
        webTestClient.post().uri("/logout")
                .cookie("SESSION", authenticatedSession)
                .body(BodyInserters.fromFormData("_csrf", csrf))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location(SecurityConfig.LOGOUT_SUCCESS_URL)
                .expectCookie().maxAge("SESSION", Duration.ZERO);

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION", authenticatedSession)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login?required");
    }

    @Test
    void formLogin_withWrongPassword_isRejected() {
        Session session = openLoginPage();

        webTestClient.post().uri("/login")
                .cookie("SESSION", session.id())
                .body(BodyInserters.fromFormData("username", "alice")
                        .with("password", "wrong")
                        .with("_csrf", session.csrf()))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/login?error");
    }

    @Test
    void formLogin_withoutCsrfToken_isForbidden() {
        webTestClient.post().uri("/login")
                .body(BodyInserters.fromFormData("username", "alice").with("password", "alice123"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void differentUsers_seeOnlyTheirOwnCarts() {
        long ballId = createItem("Мяч alice", 100).getId();
        long ropeId = createItem("Скакалка bob", 200).getId();
        asAlice().post().uri("/cart/items?id=" + ballId + "&action=PLUS").exchange().expectStatus().isOk();
        as("bob").post().uri("/cart/items?id=" + ropeId + "&action=PLUS").exchange().expectStatus().isOk();

        assertThat(html(asAlice().get().uri("/cart/items").exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult()))
                .contains("Мяч alice").doesNotContain("Скакалка bob");
        assertThat(html(as("bob").get().uri("/cart/items").exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult()))
                .contains("Скакалка bob").doesNotContain("Мяч alice");
    }

    private Session openLoginPage() {
        EntityExchangeResult<String> result = webTestClient.get().uri("/login").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();
        ResponseCookie cookie = result.getResponseCookies().getFirst("SESSION");
        assertThat(cookie).isNotNull();
        return new Session(cookie.getValue(), csrf(result.getResponseBody()));
    }

    private String login(Session session, String username, String password) {
        EntityExchangeResult<Void> result = webTestClient.post().uri("/login")
                .cookie("SESSION", session.id())
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password)
                        .with("_csrf", session.csrf()))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/")
                .expectBody().isEmpty();
        ResponseCookie renewed = result.getResponseCookies().getFirst("SESSION");
        return renewed == null ? session.id() : renewed.getValue();
    }

    private String page(String sessionId, String uri) {
        return html(webTestClient.get().uri(uri).cookie("SESSION", sessionId).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult());
    }

    private static String html(EntityExchangeResult<String> result) {
        return result.getResponseBody();
    }

    private static String csrf(String html) {
        Matcher matcher = CSRF.matcher(html);
        assertThat(matcher.find()).as("CSRF token on page").isTrue();
        return matcher.group(1);
    }

    private record Session(String id, String csrf) {
    }
}
