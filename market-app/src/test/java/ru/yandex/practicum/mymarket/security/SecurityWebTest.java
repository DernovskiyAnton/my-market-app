package ru.yandex.practicum.mymarket.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.item.SortType;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SecurityWebTest extends ControllerTestBase {

    @Test
    void loginPage_isAvailableAnonymouslyWithCsrfToken() {
        webTestClient.get().uri("/login").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("name=\"username\"", "name=\"password\"", "name=\"_csrf\"",
                        "action=\"/login\""));
    }

    @Test
    void loginPage_showsReasonOfRedirectAndLoginError() {
        webTestClient.get().uri("/login?required").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Войдите, чтобы открыть корзину"));
        webTestClient.get().uri("/login?error").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Неверный логин или пароль"));
    }

    @Test
    void accessDeniedPage_rendersForbidden() {
        asAlice().get().uri("/access-denied").exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).value(html -> assertThat(html).contains(LoginController.ACCESS_DENIED_MESSAGE));
    }

    @Test
    void header_showsImportLinkOnlyToAdmin() {
        when(itemService.findItems(any(), anyString(), any(SortType.class), anyInt(), anyInt()))
                .thenReturn(Mono.just(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0)));

        assertThat(html(asAdmin(), "/items")).contains("href=\"/admin/items\"", "admin");
        assertThat(html(asAlice(), "/items")).doesNotContain("href=\"/admin/items\"");
        assertThat(html(webTestClient, "/items")).doesNotContain("href=\"/admin/items\"");
    }

    @Test
    void logout_redirectsToCatalogAndClearsAllCookies() {
        asAlice().post().uri("/logout")
                .cookie("SESSION", "session-id")
                .cookie("theme", "dark")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location(SecurityConfig.LOGOUT_SUCCESS_URL)
                .expectCookie().maxAge("SESSION", Duration.ZERO)
                .expectCookie().maxAge("theme", Duration.ZERO);
    }

    @Test
    void logout_withoutCsrfToken_isForbidden() {
        webTestClient.post().uri("/logout").exchange()
                .expectStatus().isForbidden();
    }

    private static String html(WebTestClient client, String uri) {
        return client.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
