package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ItemControllerTest extends ControllerTestBase {

    private static final ItemDto BALL = new ItemDto(1L, "Мяч", "Футбольный мяч", "images/ball.svg", 2500, 2);

    @Test
    void getItems_withDefaults_rendersFirstPage() throws Exception {
        when(itemService.findItems("", SortType.NO, 1, 5))
                .thenReturn(new PageImpl<>(List.of(BALL), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("items", List.of(List.of(BALL, ItemDto.stub(), ItemDto.stub()))))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", "NO"))
                .andExpect(model().attribute("paging", new Paging(5, 1, false, false)))
                .andExpect(content().string(containsString("Футбольный мяч")));
    }

    @Test
    void getItems_withParams_passesThemToService() throws Exception {
        when(itemService.findItems("мяч", SortType.PRICE, 2, 2))
                .thenReturn(new PageImpl<>(List.of(BALL, BALL), PageRequest.of(1, 2), 6));

        mockMvc.perform(get("/items")
                        .param("search", "мяч")
                        .param("sort", "PRICE")
                        .param("pageNumber", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(model().attribute("search", "мяч"))
                .andExpect(model().attribute("sort", "PRICE"))
                .andExpect(model().attribute("paging", new Paging(2, 2, true, true)));
    }

    @Test
    void getItems_invalidPaging_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/items").param("pageSize", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/items").param("pageNumber", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemService);
    }

    @Test
    void getItems_unknownSort_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/items").param("sort", "RANDOM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeCartItemFromCatalog_redirectsBackWithParams() throws Exception {
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .param("search", "мяч")
                        .param("sort", "ALPHA")
                        .param("pageNumber", "3")
                        .param("pageSize", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=%D0%BC%D1%8F%D1%87&sort=ALPHA&pageNumber=3&pageSize=10"));

        verify(cartService).changeQuantity(1L, CartAction.PLUS);
    }

    @Test
    void changeCartItemFromCatalog_withoutAction_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/items").param("id", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void getItem_rendersItemPage() throws Exception {
        when(itemService.getItem(1L)).thenReturn(BALL);

        mockMvc.perform(get("/items/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", BALL))
                .andExpect(content().string(containsString("2500 руб.")));
    }

    @Test
    void getItem_unknownId_returnsNotFound() throws Exception {
        when(itemService.getItem(99L)).thenThrow(NotFoundException.item(99L));

        mockMvc.perform(get("/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name(GlobalExceptionHandler.ERROR_VIEW))
                .andExpect(model().attribute("message", "Товар с id=99 не найден"))
                .andExpect(content().string(containsString("Товар с id=99 не найден")));
    }

    @Test
    void changeCartItemFromItemPage_rendersUpdatedItem() throws Exception {
        when(itemService.getItem(1L)).thenReturn(BALL);

        mockMvc.perform(post("/items/1").param("action", "MINUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attribute("item", BALL));

        verify(cartService).changeQuantity(1L, CartAction.MINUS);
    }

    @Test
    void changeCartItemFromItemPage_unknownId_returnsNotFound() throws Exception {
        doThrow(NotFoundException.item(99L)).when(cartService).changeQuantity(99L, CartAction.PLUS);

        mockMvc.perform(post("/items/99").param("action", "PLUS"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getItems_nonNumericPageSize_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/items").param("pageSize", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name(GlobalExceptionHandler.ERROR_VIEW));

        verify(itemService, never()).findItems(anyString(), any(), anyInt(), anyInt());
    }
}
