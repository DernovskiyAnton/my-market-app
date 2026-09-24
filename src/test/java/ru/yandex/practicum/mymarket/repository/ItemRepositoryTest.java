package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends RepositoryTestBase {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void search_findsByTitleIgnoringCase() {
        Page<Item> page = itemRepository.search("МЯЧ", PageRequest.of(0, 10, SortType.ALPHA.toSort()));

        assertThat(page.getContent()).extracting(Item::getTitle)
                .containsExactly("Баскетбольный мяч", "Футбольный мяч");
    }

    @Test
    void search_findsByDescription() {
        Page<Item> page = itemRepository.search("графит", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Item::getTitle).containsExactly("Теннисная ракетка");
    }

    @Test
    void search_noMatches_returnsEmptyPage() {
        assertThat(itemRepository.search("несуществующий", PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void findAll_sortsByPriceAndPaginates() {
        Page<Item> firstPage = itemRepository.findAll(PageRequest.of(0, 5, SortType.PRICE.toSort()));
        Page<Item> lastPage = itemRepository.findAll(PageRequest.of(2, 5, SortType.PRICE.toSort()));

        assertThat(firstPage.getTotalElements()).isEqualTo(12);
        assertThat(firstPage.getContent()).hasSize(5)
                .isSortedAccordingTo(Comparator.comparingLong(Item::getPrice));
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(lastPage.getContent()).hasSize(2);
        assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    void save_persistsNewItem() {
        Item saved = itemRepository.save(new Item("Новый товар", "Описание", null, 999));
        entityManager.flush();
        entityManager.clear();

        assertThat(itemRepository.findById(saved.getId())).get()
                .extracting(Item::getTitle, Item::getPrice)
                .containsExactly("Новый товар", 999L);
    }
}
