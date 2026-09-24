package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.item.Paging;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MarketFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void catalog_searchSortAndPaging() throws Exception {
        long cheapId = createItem("Зелёный тестовый товар", 100).getId();
        long middleId = createItem("Алый тестовый товар", 200).getId();
        long expensiveId = createItem("Белый тестовый товар", 300).getId();
        long otherId = createItem("Бежевый тестовый товар", 400).getId();

        mockMvc.perform(get("/items").param("search", "тестовый").param("sort", "PRICE").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", List.of(
                        List.of(item(cheapId, "Зелёный тестовый товар", 100), item(middleId, "Алый тестовый товар", 200),
                                item(expensiveId, "Белый тестовый товар", 300)),
                        List.of(item(otherId, "Бежевый тестовый товар", 400), ItemDto.stub(), ItemDto.stub()))))
                .andExpect(model().attribute("paging", new Paging(5, 1, false, false)));

        mockMvc.perform(get("/items").param("search", "тестовый").param("sort", "ALPHA")
                        .param("pageSize", "2").param("pageNumber", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", List.of(List.of(
                        item(expensiveId, "Белый тестовый товар", 300), item(cheapId, "Зелёный тестовый товар", 100),
                        ItemDto.stub()))))
                .andExpect(model().attribute("paging", new Paging(2, 2, true, false)))
                .andExpect(content().string(containsString("Страница: 2")));
    }

    @Test
    void purchaseFlow_fromCatalogToOrder() throws Exception {
        long ballId = createItem("Тестовый мяч", 1000).getId();
        long ropeId = createItem("Тестовая скакалка", 300).getId();

        mockMvc.perform(post("/items").param("id", String.valueOf(ballId)).param("action", "PLUS"))
                .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));
        mockMvc.perform(post("/items/" + ballId).param("action", "PLUS"))
                .andExpect(view().name("item"))
                .andExpect(content().string(containsString("<span>2</span>")));
        mockMvc.perform(post("/cart/items").param("id", String.valueOf(ropeId)).param("action", "PLUS"))
                .andExpect(view().name("cart"));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(2)))
                .andExpect(model().attribute("total", 2 * 1000L + 300))
                .andExpect(content().string(containsString("Итого: 2300 руб.")));

        MvcResult buyResult = mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*?newOrder=true"))
                .andReturn();
        String orderUrl = buyResult.getResponse().getRedirectedUrl();

        mockMvc.perform(get(orderUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(content().string(containsString("Успешная покупка")))
                .andExpect(content().string(containsString("Сумма: 2300 руб.")));

        mockMvc.perform(get("/cart/items"))
                .andExpect(model().attribute("items", List.of()));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Тестовый мяч (2 шт.) 2000 руб.")));
    }

    @Test
    void buy_withEmptyCart_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/buy")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownItemAndOrder_returnNotFound() throws Exception {
        mockMvc.perform(get("/items/100500")).andExpect(status().isNotFound());
        mockMvc.perform(post("/cart/items").param("id", "100500").param("action", "PLUS"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/orders/100500")).andExpect(status().isNotFound());
    }

    @Test
    void bundledImage_isServed() throws Exception {
        mockMvc.perform(get("/images/ball.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("image/svg+xml")));
    }

    private static ItemDto item(long id, String title, long price) {
        return new ItemDto(id, title, "Описание: " + title, null, price, 0);
    }
}
