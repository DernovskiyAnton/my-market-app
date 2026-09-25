package ru.yandex.practicum.mymarket.itemimport;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.GlobalExceptionHandler;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemImportControllerTest extends ControllerTestBase {

    @Test
    void getImportPage_rendersForm() {
        webTestClient.get().uri("/admin/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Загрузка товаров на витрину").doesNotContain("alert-"));
    }

    @Test
    void getImportPage_afterImport_showsMessage() {
        webTestClient.get().uri("/admin/items?imported=3").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertThat(html).contains("Добавлено товаров: 3"));
    }

    @Test
    void importItems_success_redirectsWithImportedCount() {
        when(itemImportService.importItems(any(), anyList())).thenReturn(Mono.just(2));

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart(true)))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/admin/items?imported=2");

        verify(itemImportService).importItems(argThat(file -> "items.csv".equals(file.filename())),
                argThat((List<FilePart> images) -> images.size() == 1 && "ball.png".equals(images.get(0).filename())));
    }

    @Test
    void importItems_withoutImages_passesEmptyList() {
        when(itemImportService.importItems(any(), anyList())).thenReturn(Mono.just(1));

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart(false)))
                .exchange()
                .expectStatus().is3xxRedirection();

        verify(itemImportService).importItems(any(), argThat(List::isEmpty));
    }

    @Test
    void importItems_invalidFile_rendersImportPageWithError() {
        when(itemImportService.importItems(any(), anyList()))
                .thenReturn(Mono.error(new ItemImportException("Строка 1: ошибка")));

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart(false)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Загрузка товаров на витрину", "Строка 1: ошибка"));
    }

    @Test
    void importItems_uploadTooLarge_rendersImportPageWithError() {
        when(itemImportService.importItems(any(), anyList()))
                .thenReturn(Mono.error(new DataBufferLimitException("too large")));

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart(false)))
                .exchange()
                .expectStatus().isEqualTo(413)
                .expectBody(String.class)
                .value(html -> assertThat(html).contains(GlobalExceptionHandler.UPLOAD_TOO_LARGE_MESSAGE));
    }

    @Test
    void importItems_missingFile_rendersBadRequestPage() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("images", new byte[]{1}).filename("ball.png");

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains(GlobalExceptionHandler.BAD_REQUEST_MESSAGE));

        verifyNoInteractions(itemImportService);
    }

    private static MultiValueMap<String, HttpEntity<?>> multipart(boolean withImage) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "Мяч;100;;".getBytes(StandardCharsets.UTF_8))
                .filename("items.csv")
                .contentType(MediaType.TEXT_PLAIN);
        if (withImage) {
            builder.part("images", new byte[]{1}).filename("ball.png").contentType(MediaType.IMAGE_PNG);
        }
        return builder.build();
    }
}
