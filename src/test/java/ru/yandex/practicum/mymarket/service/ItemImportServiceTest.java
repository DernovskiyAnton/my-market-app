package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.yandex.practicum.mymarket.exception.ItemImportException;
import ru.yandex.practicum.mymarket.image.ImageService;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    void importItems_parsesCsvAndSavesItems() {
        MockMultipartFile csv = csv("""
                title;price;image;description
                Мяч;2500;ball.jpg;Кожаный мяч; размер 5

                Скакалка;600;;
                Кепка;300;cap.png
                """);
        MockMultipartFile image = new MockMultipartFile("images", "ball.jpg", "image/jpeg", new byte[]{1});

        int imported = itemImportService.importItems(csv, List.of(image));

        assertThat(imported).isEqualTo(3);
        verify(imageService).store(image);
        ArgumentCaptor<List<Item>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Item::getTitle, Item::getPrice, Item::getImgPath, Item::getDescription)
                .containsExactly(
                        tuple("Мяч", 2500L, "images/ball.jpg", "Кожаный мяч; размер 5"),
                        tuple("Скакалка", 600L, null, ""),
                        tuple("Кепка", 300L, "images/cap.png", ""));
    }

    @Test
    void importItems_emptyCsv_throwsException() {
        MockMultipartFile csv = new MockMultipartFile("file", "items.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> itemImportService.importItems(csv, List.of()))
                .isInstanceOf(ItemImportException.class);
        verifyNoInteractions(imageService, itemRepository);
    }

    @Test
    void importItems_invalidPrice_throwsExceptionWithLineNumber() {
        MockMultipartFile csv = csv("Мяч;дорого;ball.jpg;описание");

        assertThatThrownBy(() -> itemImportService.importItems(csv, List.of()))
                .isInstanceOf(ItemImportException.class)
                .hasMessageContaining("Строка 1");
        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    void importItems_negativePrice_throwsException() {
        MockMultipartFile csv = csv("Мяч;-5;;");

        assertThatThrownBy(() -> itemImportService.importItems(csv, List.of()))
                .isInstanceOf(ItemImportException.class)
                .hasMessageContaining("отрицательной");
    }

    @Test
    void importItems_notEnoughFields_throwsException() {
        MockMultipartFile csv = csv("title;price;image;description\nМяч;100");

        assertThatThrownBy(() -> itemImportService.importItems(csv, List.of()))
                .isInstanceOf(ItemImportException.class)
                .hasMessageContaining("Строка 2");
    }

    @Test
    void importItems_blankTitle_throwsException() {
        MockMultipartFile csv = csv(" ;100;;описание");

        assertThatThrownBy(() -> itemImportService.importItems(csv, List.of()))
                .isInstanceOf(ItemImportException.class)
                .hasMessageContaining("название");
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "items.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
