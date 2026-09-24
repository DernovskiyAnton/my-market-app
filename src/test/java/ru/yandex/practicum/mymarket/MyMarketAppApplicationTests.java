package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.item.ItemController;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class MyMarketAppApplicationTests extends IntegrationTestBase {

    @Autowired
    private ItemController itemController;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void contextLoads() {
        assertThat(itemController).isNotNull();
    }

    @Test
    void initialCatalogIsLoaded() {
        assertThat(itemRepository.count()).isEqualTo(12);
    }
}
