package ru.yandex.practicum.mymarket.image;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.time.Duration;

import static org.mockito.Mockito.when;

class ImageControllerTest extends ControllerTestBase {

    @Test
    void getImage_returnsImageWithContentType() {
        when(imageService.load("ball.png")).thenReturn(Mono.just(new ByteArrayResource(new byte[]{1, 2, 3})));

        webTestClient.get().uri("/images/ball.png").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .expectBody(byte[].class).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void getImage_missing_returnsNotFound() {
        when(imageService.load("missing.png")).thenReturn(Mono.empty());

        webTestClient.get().uri("/images/missing.png").exchange()
                .expectStatus().isNotFound();
    }
}
