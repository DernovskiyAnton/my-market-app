package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.common.GlobalExceptionHandler;
import ru.yandex.practicum.mymarket.support.ControllerTestBase;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class PurchaseControllerTest extends ControllerTestBase {

    @Test
    void buy_redirectsToNewOrder() throws Exception {
        when(purchaseService.buy()).thenReturn(7L);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/7?newOrder=true"));
    }

    @Test
    void buy_emptyCart_returnsBadRequest() throws Exception {
        when(purchaseService.buy()).thenThrow(new EmptyCartException());

        mockMvc.perform(post("/buy"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name(GlobalExceptionHandler.ERROR_VIEW))
                .andExpect(model().attribute("message", "Невозможно оформить заказ: корзина пуста"));
    }
}
