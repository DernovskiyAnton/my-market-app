package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class ItemGridTest {

    @Test
    void toRows_splitsItemsByThreeAndPadsLastRowWithStubs() {
        List<ItemDto> items = LongStream.rangeClosed(1, 4).mapToObj(ItemGridTest::item).toList();

        List<List<ItemDto>> rows = ItemGrid.toRows(items);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).extracting(ItemDto::id).containsExactly(1L, 2L, 3L);
        assertThat(rows.get(1)).extracting(ItemDto::id).containsExactly(4L, ItemDto.STUB_ID, ItemDto.STUB_ID);
    }

    @Test
    void toRows_fullRows_haveNoStubs() {
        List<ItemDto> items = LongStream.rangeClosed(1, 6).mapToObj(ItemGridTest::item).toList();

        assertThat(ItemGrid.toRows(items)).hasSize(2)
                .allSatisfy(row -> assertThat(row).noneMatch(item -> item.id() == ItemDto.STUB_ID));
    }

    @Test
    void toRows_emptyList_returnsNoRows() {
        assertThat(ItemGrid.toRows(List.of())).isEmpty();
    }

    private static ItemDto item(long id) {
        return new ItemDto(id, "t" + id, "d", "images/x.jpg", 10, 0);
    }
}
