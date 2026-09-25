package ru.yandex.practicum.mymarket.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;

@SpringBootTest(properties = "market.images.dir=target/test-images")
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    protected Item createItem(String title, long price) {
        return itemRepository.save(new Item(title, "Описание: " + title, null, price));
    }
}
