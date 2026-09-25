package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends RepositoryTestBase {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void save_persistsNewItem() {
        Item saved = itemRepository.save(new Item("Новый товар", "Описание", "images/new.png", 999)).block();

        StepVerifier.create(itemRepository.findById(saved.getId()))
                .assertNext(item -> {
                    assertThat(item.getTitle()).isEqualTo("Новый товар");
                    assertThat(item.getDescription()).isEqualTo("Описание");
                    assertThat(item.getImgPath()).isEqualTo("images/new.png");
                    assertThat(item.getPrice()).isEqualTo(999L);
                })
                .verifyComplete();
    }

    @Test
    void findAll_sortedById_returnsItemsInInsertionOrder() {
        itemRepository.saveAll(Flux.just(
                new Item("Первый", "", null, 30),
                new Item("Второй", "", null, 10),
                new Item("Третий", "", null, 20)))
                .blockLast();

        StepVerifier.create(itemRepository.findAll(Sort.by("id")).map(Item::getTitle))
                .expectNext("Первый", "Второй", "Третий")
                .verifyComplete();
    }

    @Test
    void findAllById_returnsOnlyRequestedItems() {
        List<Long> ids = itemRepository.saveAll(Flux.just(
                new Item("Первый", "", null, 30),
                new Item("Второй", "", null, 10),
                new Item("Третий", "", null, 20)))
                .map(Item::getId)
                .collectList()
                .block();

        StepVerifier.create(itemRepository.findAllById(List.of(ids.get(0), ids.get(2))).map(Item::getTitle).sort())
                .expectNext("Первый", "Третий")
                .verifyComplete();
    }

    @Test
    void save_negativePrice_violatesCheckConstraint() {
        StepVerifier.create(itemRepository.save(new Item("Товар", "", null, -1)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}
