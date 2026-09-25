package ru.yandex.practicum.mymarket.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(properties = "market.images.dir=target/test-images")
@AutoConfigureWebTestClient
public abstract class IntegrationTestBase {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected DatabaseClient databaseClient;

    @Autowired
    private ItemRepository itemRepository;

    private final List<Long> createdItemIds = new ArrayList<>();

    @AfterEach
    void cleanDatabase() {
        Flux.just("order_items", "orders", "cart_items")
                .concatMap(table -> databaseClient.sql("DELETE FROM " + table).then())
                .then(itemRepository.deleteAllById(createdItemIds))
                .block();
        createdItemIds.clear();
    }

    protected Item createItem(String title, long price) {
        Item item = itemRepository.save(new Item(title, "Описание: " + title, null, price)).block();
        createdItemIds.add(item.getId());
        return item;
    }

    protected void trackCreatedItems(String titlePart) {
        itemRepository.search(titlePart, Pageable.unpaged())
                .map(Item::getId)
                .doOnNext(createdItemIds::add)
                .blockLast();
    }
}
