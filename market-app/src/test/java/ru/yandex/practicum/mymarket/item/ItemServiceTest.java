package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final List<ItemSummary> CATALOG = List.of(
            new ItemSummary(1L, "Футбольный мяч", "Кожаный, размер 5", 2500),
            new ItemSummary(2L, "скакалка", "Скоростная", 600),
            new ItemSummary(3L, "Баскетбольный мяч", "Для зала", 3200),
            new ItemSummary(4L, "Ракетка", "Лёгкая, из графита", 5400),
            new ItemSummary(5L, "Бутылка", "Для воды", 450));

    @Mock
    private ItemCache itemCache;

    @Mock
    private CartService cartService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void findItems_withoutSearch_returnsFirstPageInIdOrderWithCartQuantities() {
        givenCatalog();
        givenCardsAndQuantities(List.of(1L, 2L), Map.of(2L, 3));

        StepVerifier.create(itemService.findItems("", SortType.NO, 1, 2))
                .assertNext(page -> {
                    assertThat(page.getContent()).extracting(ItemDto::id).containsExactly(1L, 2L);
                    assertThat(page.getContent()).extracting(ItemDto::count).containsExactly(0, 3);
                    assertThat(page.getContent()).extracting(ItemDto::imgPath)
                            .containsExactly("images/1.jpg", "images/2.jpg");
                    assertThat(page.getTotalElements()).isEqualTo(5);
                    assertThat(page.hasPrevious()).isFalse();
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findItems_searchesTitleAndDescriptionIgnoringCase() {
        givenCatalog();
        givenCardsAndQuantities(List.of(3L, 1L), Map.of());

        StepVerifier.create(itemService.findItems(" МЯЧ ", SortType.ALPHA, 1, 10))
                .assertNext(page -> {
                    assertThat(page.getContent()).extracting(ItemDto::title)
                            .containsExactly("Баскетбольный мяч", "Футбольный мяч");
                    assertThat(page.getTotalElements()).isEqualTo(2);
                })
                .verifyComplete();

        givenCardsAndQuantities(List.of(4L), Map.of());
        StepVerifier.create(itemService.findItems("графит", SortType.NO, 1, 10))
                .assertNext(page -> assertThat(page.getContent()).extracting(ItemDto::id).containsExactly(4L))
                .verifyComplete();
    }

    @Test
    void findItems_sortsByPriceAndReturnsRequestedPage() {
        givenCatalog();
        givenCardsAndQuantities(List.of(1L, 3L), Map.of());

        StepVerifier.create(itemService.findItems("", SortType.PRICE, 2, 2))
                .assertNext(page -> {
                    assertThat(page.getContent()).extracting(ItemDto::price).containsExactly(2500L, 3200L);
                    assertThat(page.getNumber()).isEqualTo(1);
                    assertThat(page.hasPrevious()).isTrue();
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findItems_sortsAlphabeticallyIgnoringCase() {
        givenCatalog();
        givenCardsAndQuantities(List.of(3L, 5L, 4L, 2L, 1L), Map.of());

        StepVerifier.create(itemService.findItems("", SortType.ALPHA, 1, 10))
                .assertNext(page -> assertThat(page.getContent()).extracting(ItemDto::title).containsExactly(
                        "Баскетбольный мяч", "Бутылка", "Ракетка", "скакалка", "Футбольный мяч"))
                .verifyComplete();
    }

    @Test
    void findItems_pageAfterLast_returnsEmptyContent() {
        givenCatalog();
        givenCardsAndQuantities(List.of(), Map.of());

        StepVerifier.create(itemService.findItems("", SortType.NO, 4, 2))
                .assertNext(page -> {
                    assertThat(page.getContent()).isEmpty();
                    assertThat(page.hasNext()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void getItem_returnsCachedCardWithCartQuantity() {
        when(itemCache.getCard(1L)).thenReturn(Mono.just(card(1L, "Футбольный мяч", 2500)));
        when(cartService.getQuantity(1L)).thenReturn(Mono.just(3));

        StepVerifier.create(itemService.getItem(1L))
                .expectNext(new ItemDto(1L, "Футбольный мяч", "Описание 1", "images/1.jpg", 2500, 3))
                .verifyComplete();
    }

    @Test
    void getItem_unknownId_returnsNotFound() {
        when(itemCache.getCard(99L)).thenReturn(Mono.empty());
        when(cartService.getQuantity(99L)).thenReturn(Mono.just(0));

        StepVerifier.create(itemService.getItem(99L))
                .expectError(NotFoundException.class)
                .verify();
    }

    private void givenCatalog() {
        when(itemCache.getSummaries()).thenReturn(Mono.just(CATALOG));
    }

    private void givenCardsAndQuantities(List<Long> ids, Map<Long, Integer> quantities) {
        Map<Long, ItemCard> cards = CATALOG.stream()
                .filter(summary -> ids.contains(summary.id()))
                .collect(Collectors.toMap(ItemSummary::id,
                        summary -> card(summary.id(), summary.title(), summary.price())));
        when(itemCache.getCards(ids)).thenReturn(Mono.just(cards));
        when(cartService.getQuantities(ids)).thenReturn(Mono.just(quantities));
    }

    private static ItemCard card(long id, String title, long price) {
        return new ItemCard(id, title, "Описание " + id, "images/" + id + ".jpg", price);
    }
}
