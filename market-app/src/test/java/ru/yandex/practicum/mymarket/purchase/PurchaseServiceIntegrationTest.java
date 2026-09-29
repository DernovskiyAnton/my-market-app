package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.order.OrderDto;
import ru.yandex.practicum.mymarket.order.OrderItemDto;
import ru.yandex.practicum.mymarket.order.OrderService;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        cartService.changeQuantity(ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(ballId, CartAction.PLUS))
                .then(cartService.changeQuantity(racketId, CartAction.PLUS))
                .block();

        Long orderId = purchaseService.buy().block();

        StepVerifier.create(orderService.getOrder(orderId))
                .expectNext(new OrderDto(orderId, List.of(
                        new OrderItemDto(ballId, "Тестовый мяч", 1000, 2),
                        new OrderItemDto(racketId, "Тестовая ракетка", 4000, 1)), 2 * 1000 + 4000))
                .verifyComplete();
        StepVerifier.create(cartService.getCart())
                .assertNext(cart -> assertThat(cart.items()).isEmpty())
                .verifyComplete();
    }

    @Test
    void buy_emptyCart_returnsError() {
        StepVerifier.create(purchaseService.buy())
                .expectError(EmptyCartException.class)
                .verify();
    }

    @Test
    void buy_severalTimes_ordersListedNewestFirst() {
        Long first = cartService.changeQuantity(ballId, CartAction.PLUS).then(purchaseService.buy()).block();
        Long second = cartService.changeQuantity(racketId, CartAction.PLUS).then(purchaseService.buy()).block();

        StepVerifier.create(orderService.findAll().map(OrderDto::id))
                .expectNext(second, first)
                .verifyComplete();
    }
}
