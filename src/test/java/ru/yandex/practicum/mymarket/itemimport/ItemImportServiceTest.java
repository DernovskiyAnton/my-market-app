package ru.yandex.practicum.mymarket.itemimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.image.ImageService;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemImportServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ItemImportService itemImportService;

    @Test
    @SuppressWarnings("unchecked")
    void importItems_parsesCsvSavesItemsAndThenStoresImages() {
        FilePart csv = filePart("items.csv", """
                title;price;image;description
                Мяч;2500;ball.jpg;Кожаный мяч; размер 5

                Скакалка;600;;
                Кепка;300;cap.png
                """);
        FilePart image = filePart("ball.jpg", "image");
        FilePart notSelected = filePart("", "");
        when(itemRepository.saveAll(anyList())).thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));
        when(imageService.store(eq("ball.jpg"), any())).thenReturn(Mono.just("images/ball.jpg"));

        StepVerifier.create(itemImportService.importItems(csv, List.of(image, notSelected)))
                .expectNext(3)
                .verifyComplete();

        ArgumentCaptor<List<Item>> captor = ArgumentCaptor.forClass(List.class);
        InOrder inOrder = inOrder(itemRepository, imageService);
        inOrder.verify(itemRepository).saveAll(captor.capture());
        inOrder.verify(imageService).store(eq("ball.jpg"), any());
        verify(imageService, never()).store(eq(""), any());
        assertThat(captor.getValue())
                .extracting(Item::getTitle, Item::getPrice, Item::getImgPath, Item::getDescription)
                .containsExactly(
                        tuple("Мяч", 2500L, "images/ball.jpg", "Кожаный мяч; размер 5"),
                        tuple("Скакалка", 600L, null, ""),
                        tuple("Кепка", 300L, "images/cap.png", ""));
    }

    @Test
    void importItems_emptyCsv_returnsError() {
        StepVerifier.create(itemImportService.importItems(filePart("items.csv", ""), List.of()))
                .expectErrorMessage("Выберите CSV-файл со списком товаров")
                .verify();
        verifyNoInteractions(imageService, itemRepository);
    }

    @Test
    void importItems_invalidCsv_doesNotSaveItemsOrStoreImages() {
        FilePart csv = filePart("items.csv", "Мяч;100;ball.jpg;описание\nКепка;дорого;cap.png;описание");

        StepVerifier.create(itemImportService.importItems(csv, List.of(filePart("ball.jpg", "image"))))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ItemImportException.class)
                        .hasMessageContaining("Строка 2"))
                .verify();
        verifyNoInteractions(imageService, itemRepository);
    }

    @Test
    void parse_negativePrice_throwsException() {
        assertThatImportFails("Мяч;-5;;", "отрицательной");
    }

    @Test
    void parse_notEnoughFields_throwsException() {
        assertThatImportFails("title;price;image;description\nМяч;100", "Строка 2");
    }

    @Test
    void parse_blankTitle_throwsException() {
        assertThatImportFails(" ;100;;описание", "название");
    }

    private void assertThatImportFails(String content, String messagePart) {
        assertThatThrownBy(() -> itemImportService.parse(content))
                .isInstanceOf(ItemImportException.class)
                .hasMessageContaining(messagePart);
    }

    private static FilePart filePart(String fileName, String content) {
        FilePart part = mock(FilePart.class);
        lenient().when(part.filename()).thenReturn(fileName);
        lenient().when(part.content()).thenReturn(content.isEmpty()
                ? Flux.empty()
                : Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(content.getBytes(StandardCharsets.UTF_8))));
        return part;
    }
}
