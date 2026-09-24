package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void findItems_withoutSearch_returnsAllItemsWithCartQuantities() {
        PageRequest pageable = PageRequest.of(1, 2, Sort.by("price").ascending());
        Page<Item> page = new PageImpl<>(List.of(item(3L, 30), item(4L, 40)), pageable, 5);
        when(itemRepository.findAll(pageable)).thenReturn(page);
        when(cartService.getQuantities(List.of(3L, 4L))).thenReturn(Map.of(4L, 2));

        Page<ItemDto> result = itemService.findItems("  ", SortType.PRICE, 2, 2);

        verify(itemRepository, never()).search(anyString(), any());
        assertThat(result.getContent()).extracting(ItemDto::id).containsExactly(3L, 4L);
        assertThat(result.getContent()).extracting(ItemDto::count).containsExactly(0, 2);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void findItems_withSearch_usesTrimmedSearchString() {
        PageRequest pageable = PageRequest.of(0, 5, Sort.unsorted());
        when(itemRepository.search("мяч", pageable)).thenReturn(new PageImpl<>(List.of(item(1L, 10)), pageable, 1));
        when(cartService.getQuantities(List.of(1L))).thenReturn(Map.of());

        Page<ItemDto> result = itemService.findItems(" мяч ", SortType.NO, 1, 5);

        assertThat(result.getContent()).singleElement().extracting(ItemDto::title).isEqualTo("Товар 1");
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getItem_returnsItemWithCartQuantity() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 100)));
        when(cartService.getQuantity(1L)).thenReturn(3);

        ItemDto item = itemService.getItem(1L);

        assertThat(item).isEqualTo(new ItemDto(1L, "Товар 1", "Описание 1", "images/1.jpg", 100, 3));
    }

    @Test
    void getItem_unknownId_throwsNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(99L)).isInstanceOf(NotFoundException.class);
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
