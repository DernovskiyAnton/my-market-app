package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.item.ItemController;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class MyMarketAppApplicationTests extends IntegrationTestBase {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void contextLoads() {
        assertThat(applicationContext.getBean(ItemController.class)).isNotNull();
    }

    @Test
    void initialCatalogIsLoaded() {
        StepVerifier.create(itemRepository.count())
                .assertNext(count -> assertThat(count).isPositive())
                .verifyComplete();
    }
}
