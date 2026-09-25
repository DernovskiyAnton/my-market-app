package ru.yandex.practicum.mymarket.itemimport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.item.ItemService;
import ru.yandex.practicum.mymarket.item.SortType;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ItemImportIntegrationTest extends IntegrationTestBase {

    private static final String MARKER = "Импортированный";

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Value("${market.images.dir}")
    private Path imagesDir;

    @AfterEach
    void trackImportedItems() {
        trackCreatedItems(MARKER);
    }

    @Test
    void importItems_addsItemsToCatalogAndStoresImages() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", """
                title;price;image;description
                Импортированный мяч;350;puck.png;Официальная шайба
                Импортированная клюшка;4200;;Деревянная клюшка
                """.getBytes(StandardCharsets.UTF_8)).filename("items.csv");
        builder.part("images", new byte[]{9, 8, 7}).filename("puck.png");

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/admin/items?imported=2");

        StepVerifier.create(itemService.findItems("импортированный мяч", SortType.NO, 1, 10))
                .assertNext(page -> assertThat(page.getContent()).singleElement()
                        .extracting(ItemDto::title, ItemDto::price, ItemDto::imgPath)
                        .containsExactly("Импортированный мяч", 350L, "images/puck.png"))
                .verifyComplete();
        webTestClient.get().uri("/images/puck.png").exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).isEqualTo(new byte[]{9, 8, 7});
    }

    @Test
    void importItems_invalidCsv_doesNotChangeCatalogAndStoreImages() {
        long before = itemRepository.count().block();
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "Импортированный товар;100;rejected.png;ок\nПлохой;не число;;"
                .getBytes(StandardCharsets.UTF_8)).filename("items.csv");
        builder.part("images", new byte[]{1}).filename("rejected.png");

        webTestClient.post().uri("/admin/items/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).value(html -> assertThat(html).contains("Строка 2"));

        assertThat(itemRepository.count().block()).isEqualTo(before);
        assertThat(imagesDir.resolve("rejected.png")).doesNotExist();
        webTestClient.get().uri("/images/rejected.png").exchange().expectStatus().isNotFound();
    }
}
