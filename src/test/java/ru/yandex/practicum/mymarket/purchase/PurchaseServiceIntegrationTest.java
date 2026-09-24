package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.order.OrderDto;
import ru.yandex.practicum.mymarket.order.OrderItemDto;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    private long ballId;
    private long racketId;

    @BeforeEach
    void setUp() {
        ballId = createItem("Тестовый мяч", 1000).getId();
        racketId = createItem("Тестовая ракетка", 4000).getId();
    }

    @Test
    void buy_savesOrderAndClearsCart() {
        cartService.changeQuantity(ballId, CartAction.PLUS);
        cartService.changeQuantity(ballId, CartAction.PLUS);
        cartService.changeQuantity(racketId, CartAction.PLUS);

        long orderId = purchaseService.buy();

        OrderDto order = orderService.getOrder(orderId);
        assertThat(order.items()).containsExactly(
                new OrderItemDto(ballId, "Тестовый мяч", 1000, 2),
                new OrderItemDto(racketId, "Тестовая ракетка", 4000, 1));
        assertThat(order.totalSum()).isEqualTo(2 * 1000 + 4000);
        assertThat(cartService.getCart().items()).isEmpty();
        assertThat(orderService.findAll()).extracting(OrderDto::id).contains(orderId);
    }

    @Test
    void buy_emptyCart_throwsException() {
        assertThatThrownBy(() -> purchaseService.buy()).isInstanceOf(EmptyCartException.class);
    }

    @Test
    void buy_severalTimes_ordersListedNewestFirst() {
        cartService.changeQuantity(ballId, CartAction.PLUS);
        long first = purchaseService.buy();
        cartService.changeQuantity(racketId, CartAction.PLUS);
        long second = purchaseService.buy();

        List<Long> ids = orderService.findAll().stream().map(OrderDto::id).toList();

        assertThat(ids).containsSubsequence(second, first);
    }
}
