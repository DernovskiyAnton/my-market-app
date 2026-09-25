package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends RepositoryTestBase {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.saveAll(Flux.just(
                new Item("Футбольный мяч", "Кожаный, размер 5", null, 2500),
                new Item("Скакалка", "Скоростная, с подшипниками", null, 600),
                new Item("Баскетбольный мяч", "Для зала и улицы", null, 3200),
                new Item("Ракетка", "Лёгкая, из графита", null, 5400),
                new Item("Бутылка", "Для воды, 750 мл", null, 450)))
                .blockLast();
    }

    @Test
    void search_findsByTitleIgnoringCase() {
        StepVerifier.create(itemRepository.search("МЯЧ", PageRequest.of(0, 10, SortType.ALPHA.toSort()))
                        .map(Item::getTitle))
                .expectNext("Баскетбольный мяч", "Футбольный мяч")
                .verifyComplete();
    }

    @Test
    void search_findsByDescription() {
        StepVerifier.create(itemRepository.search("графит", PageRequest.of(0, 10)).map(Item::getTitle))
                .expectNext("Ракетка")
                .verifyComplete();
    }

    @Test
    void search_noMatches_returnsNothing() {
        StepVerifier.create(itemRepository.search("несуществующий", PageRequest.of(0, 10)))
                .verifyComplete();
        StepVerifier.create(itemRepository.countSearch("несуществующий"))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void search_paginatesResults() {
        StepVerifier.create(itemRepository.search("для", PageRequest.of(0, 1, SortType.PRICE.toSort()))
                        .map(Item::getTitle))
                .expectNext("Бутылка")
                .verifyComplete();
        StepVerifier.create(itemRepository.search("для", PageRequest.of(1, 1, SortType.PRICE.toSort()))
                        .map(Item::getTitle))
                .expectNext("Баскетбольный мяч")
                .verifyComplete();
        StepVerifier.create(itemRepository.countSearch("для"))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void findAllBy_sortsByPriceAndPaginates() {
        StepVerifier.create(itemRepository.findAllBy(PageRequest.of(0, 2, SortType.PRICE.toSort())).map(Item::getPrice))
                .expectNext(450L, 600L)
                .verifyComplete();
        StepVerifier.create(itemRepository.findAllBy(PageRequest.of(2, 2, SortType.PRICE.toSort())).map(Item::getPrice))
                .expectNext(5400L)
                .verifyComplete();
        StepVerifier.create(itemRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    void findAllBy_sortsByTitle() {
        StepVerifier.create(itemRepository.findAllBy(PageRequest.of(0, 10, SortType.ALPHA.toSort())).map(Item::getTitle))
                .expectNext("Баскетбольный мяч", "Бутылка", "Ракетка", "Скакалка", "Футбольный мяч")
                .verifyComplete();
    }

    @Test
    void save_persistsNewItem() {
        Item saved = itemRepository.save(new Item("Новый товар", "Описание", "images/new.png", 999)).block();

        StepVerifier.create(itemRepository.findById(saved.getId()))
                .assertNext(item -> {
                    assertThat(item.getTitle()).isEqualTo("Новый товар");
                    assertThat(item.getImgPath()).isEqualTo("images/new.png");
                    assertThat(item.getPrice()).isEqualTo(999L);
                })
                .verifyComplete();
    }
}
