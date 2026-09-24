package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends RepositoryTestBase {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAllInBatch();
        persist("Футбольный мяч", "Кожаный, размер 5", 2500);
        persist("Скакалка", "Скоростная, с подшипниками", 600);
        persist("Баскетбольный мяч", "Для зала и улицы", 3200);
        persist("Ракетка", "Лёгкая, из графита", 5400);
        persist("Бутылка", "Для воды, 750 мл", 450);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void search_findsByTitleIgnoringCase() {
        Page<Item> page = itemRepository.search("МЯЧ", PageRequest.of(0, 10, SortType.ALPHA.toSort()));

        assertThat(page.getContent()).extracting(Item::getTitle)
                .containsExactly("Баскетбольный мяч", "Футбольный мяч");
    }

    @Test
    void search_findsByDescription() {
        Page<Item> page = itemRepository.search("графит", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Item::getTitle).containsExactly("Ракетка");
    }

    @Test
    void search_noMatches_returnsEmptyPage() {
        assertThat(itemRepository.search("несуществующий", PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void search_paginatesResults() {
        Page<Item> firstPage = itemRepository.search("для", PageRequest.of(0, 1, SortType.PRICE.toSort()));
        Page<Item> secondPage = itemRepository.search("для", PageRequest.of(1, 1, SortType.PRICE.toSort()));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Item::getTitle).containsExactly("Бутылка");
        assertThat(secondPage.getContent()).extracting(Item::getTitle).containsExactly("Баскетбольный мяч");
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void findAll_sortsByPriceAndPaginates() {
        Page<Item> firstPage = itemRepository.findAll(PageRequest.of(0, 2, SortType.PRICE.toSort()));
        Page<Item> lastPage = itemRepository.findAll(PageRequest.of(2, 2, SortType.PRICE.toSort()));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(Item::getPrice).containsExactly(450L, 600L);
        assertThat(firstPage.hasPrevious()).isFalse();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(lastPage.getContent()).extracting(Item::getPrice).containsExactly(5400L);
        assertThat(lastPage.hasNext()).isFalse();
    }

    @Test
    void findAll_sortsByTitle() {
        Page<Item> page = itemRepository.findAll(PageRequest.of(0, 10, SortType.ALPHA.toSort()));

        assertThat(page.getContent()).extracting(Item::getTitle).containsExactly(
                "Баскетбольный мяч", "Бутылка", "Ракетка", "Скакалка", "Футбольный мяч");
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

    private void persist(String title, String description, long price) {
        entityManager.persist(new Item(title, description, null, price));
    }
}
