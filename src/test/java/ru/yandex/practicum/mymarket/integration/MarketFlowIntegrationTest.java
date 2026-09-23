package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

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
        mockMvc.perform(get("/items").param("search", "мяч").param("sort", "PRICE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(content().string(containsString("Футбольный мяч")))
                .andExpect(content().string(containsString("Баскетбольный мяч")));

        mockMvc.perform(get("/").param("pageSize", "5").param("pageNumber", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(content().string(containsString("Страница: 3")));
    }

    @Test
    void purchaseFlow_fromCatalogToOrder() throws Exception {
        mockMvc.perform(post("/items").param("id", "1").param("action", "PLUS"))
                .andExpect(redirectedUrl("/items?search=&sort=NO&pageNumber=1&pageSize=5"));
        mockMvc.perform(post("/items/1").param("action", "PLUS"))
                .andExpect(view().name("item"))
                .andExpect(content().string(containsString("<span>2</span>")));
        mockMvc.perform(post("/cart/items").param("id", "4").param("action", "PLUS"))
                .andExpect(view().name("cart"));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("items", hasSize(2)))
                .andExpect(model().attribute("total", 2 * 2500L + 600))
                .andExpect(content().string(containsString("Итого: 5600 руб.")));

        MvcResult buyResult = mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*?newOrder=true"))
                .andReturn();
        String orderUrl = buyResult.getResponse().getRedirectedUrl();

        mockMvc.perform(get(orderUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(content().string(containsString("Успешная покупка")))
                .andExpect(content().string(containsString("Сумма: 5600 руб.")));

        mockMvc.perform(get("/cart/items"))
                .andExpect(model().attribute("items", List.of()));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Футбольный мяч (2 шт.) 5000 руб.")));
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
}
