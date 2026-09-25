package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void findItems_withoutSearch_returnsPageWithCartQuantities() {
        PageRequest pageable = PageRequest.of(1, 2, Sort.by("price").ascending());
        when(itemRepository.findAllBy(pageable)).thenReturn(Flux.just(item(3L, 30), item(4L, 40)));
        when(itemRepository.count()).thenReturn(Mono.just(5L));
        when(cartService.getQuantities(List.of(3L, 4L))).thenReturn(Mono.just(Map.of(4L, 2)));

        StepVerifier.create(itemService.findItems("  ", SortType.PRICE, 2, 2))
                .assertNext(page -> {
                    assertThat(page.getContent()).extracting(ItemDto::id).containsExactly(3L, 4L);
                    assertThat(page.getContent()).extracting(ItemDto::count).containsExactly(0, 2);
                    assertThat(page.getTotalElements()).isEqualTo(5);
                    assertThat(page.hasPrevious()).isTrue();
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
        verify(itemRepository, never()).search(anyString(), any());
    }

    @Test
    void findItems_withSearch_usesTrimmedSearchString() {
        PageRequest pageable = PageRequest.of(0, 5, Sort.unsorted());
        when(itemRepository.search("мяч", pageable)).thenReturn(Flux.just(item(1L, 10)));
        when(itemRepository.countSearch("мяч")).thenReturn(Mono.just(1L));
        when(cartService.getQuantities(List.of(1L))).thenReturn(Mono.just(Map.of()));

        StepVerifier.create(itemService.findItems(" мяч ", SortType.NO, 1, 5))
                .assertNext(page -> {
                    assertThat(page.getContent()).singleElement().extracting(ItemDto::title).isEqualTo("Товар 1");
                    assertThat(page.hasNext()).isFalse();
                })
                .verifyComplete();
        verify(itemRepository, never()).findAllBy(any());
    }

    @Test
    void getItem_returnsItemWithCartQuantity() {
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartService.getQuantity(1L)).thenReturn(Mono.just(3));

        StepVerifier.create(itemService.getItem(1L))
                .expectNext(new ItemDto(1L, "Товар 1", "Описание 1", "images/1.jpg", 100, 3))
                .verifyComplete();
    }

    @Test
    void getItem_unknownId_returnsNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());
        when(cartService.getQuantity(99L)).thenReturn(Mono.just(0));

        StepVerifier.create(itemService.getItem(99L))
                .expectError(NotFoundException.class)
                .verify();
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
