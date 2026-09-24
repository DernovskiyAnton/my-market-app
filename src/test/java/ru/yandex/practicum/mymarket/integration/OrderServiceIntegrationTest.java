package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Test
    void createOrderFromCart_savesOrderAndClearsCart() {
        cartService.changeQuantity(1L, CartAction.PLUS);
        cartService.changeQuantity(1L, CartAction.PLUS);
        cartService.changeQuantity(3L, CartAction.PLUS);

        long orderId = orderService.createOrderFromCart();

        OrderDto order = orderService.getOrder(orderId);
        assertThat(order.items()).containsExactly(
                new OrderItemDto(1L, "Футбольный мяч", 2500, 2),
                new OrderItemDto(3L, "Теннисная ракетка", 5400, 1));
        assertThat(order.totalSum()).isEqualTo(2 * 2500 + 5400);
        assertThat(cartService.getCart().items()).isEmpty();
        assertThat(orderService.findAll()).extracting(OrderDto::id).contains(orderId);
    }

    @Test
    void createOrderFromCart_emptyCart_throwsException() {
        assertThatThrownBy(() -> orderService.createOrderFromCart()).isInstanceOf(EmptyCartException.class);
    }

    @Test
    void findAll_returnsNewestOrdersFirst() {
        cartService.changeQuantity(1L, CartAction.PLUS);
        long first = orderService.createOrderFromCart();
        cartService.changeQuantity(2L, CartAction.PLUS);
        long second = orderService.createOrderFromCart();

        List<Long> ids = orderService.findAll().stream().map(OrderDto::id).toList();

        assertThat(ids).containsSubsequence(second, first);
    }
}
