package ru.yandex.practicum.mymarket.order;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class OrderControllerTest extends ControllerTestBase {

    private static final OrderDto ORDER = new OrderDto(7L,
            List.of(new OrderItemDto(1L, "Мяч", 2500, 2), new OrderItemDto(2L, "Скакалка", 600, 1)), 5600);

    @Test
    void getOrders_rendersOrderList() throws Exception {
        when(orderService.findAll()).thenReturn(List.of(ORDER));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", List.of(ORDER)))
                .andExpect(content().string(containsString("Заказ №7")))
                .andExpect(content().string(containsString("Мяч (2 шт.) 5000 руб.")))
                .andExpect(content().string(containsString("Сумма: 5600 руб.")));
    }

    @Test
    void getOrder_newOrder_showsSuccessAlert() throws Exception {
        when(orderService.getOrder(7L)).thenReturn(ORDER);

        mockMvc.perform(get("/orders/7").param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", ORDER))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(content().string(containsString("Успешная покупка")));
    }

    @Test
    void getOrder_byDefault_isNotNewOrder() throws Exception {
        when(orderService.getOrder(7L)).thenReturn(ORDER);

        mockMvc.perform(get("/orders/7"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("newOrder", false))
                .andExpect(content().string(not(containsString("Успешная покупка"))));
    }

    @Test
    void getOrder_unknownId_returnsNotFound() throws Exception {
        when(orderService.getOrder(99L)).thenThrow(NotFoundException.order(99L));

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound());
    }
}
