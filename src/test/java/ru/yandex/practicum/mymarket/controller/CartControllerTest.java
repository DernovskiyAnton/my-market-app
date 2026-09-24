package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class CartControllerTest extends ControllerTestBase {

    private static final ItemDto BALL = new ItemDto(1L, "Мяч", "Футбольный мяч", "images/ball.svg", 2500, 2);

    @Test
    void getCart_rendersItemsAndTotal() throws Exception {
        when(cartService.getCart()).thenReturn(new CartDto(List.of(BALL), 5000));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", List.of(BALL)))
                .andExpect(model().attribute("total", 5000L))
                .andExpect(content().string(containsString("Итого: 5000 руб.")));
    }

    @Test
    void getCart_empty_hidesBuyButton() throws Exception {
        when(cartService.getCart()).thenReturn(new CartDto(List.of(), 0));

        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Купить"))));
    }

    @Test
    void changeCartItem_appliesActionAndRendersCart() throws Exception {
        when(cartService.getCart()).thenReturn(new CartDto(List.of(), 0));

        mockMvc.perform(post("/cart/items").param("id", "1").param("action", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", List.of()))
                .andExpect(model().attribute("total", 0L));

        verify(cartService).changeQuantity(1L, CartAction.DELETE);
    }

    @Test
    void changeCartItem_withoutId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/cart/items").param("action", "PLUS"))
                .andExpect(status().isBadRequest());
    }
}
