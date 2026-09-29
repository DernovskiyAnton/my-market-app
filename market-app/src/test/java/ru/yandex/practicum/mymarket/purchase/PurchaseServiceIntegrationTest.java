package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartAction;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.common.NotFoundException;
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

    private long aliceId;
    private long bobId;
    private long ballId;
    private long racketId;

    @BeforeEach
    void setUp() {
        aliceId = user("alice").getId();
        bobId = user("bob").getId();
        ballId = createItem("Тестовый мяч", 1000).getId();
        racketId = createItem("Тестовая ракетка", 4000).getId();
    }

    @Test
    void buy_savesOrderAndClearsCart() {
        cartService.changeQuantity(aliceId, ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(aliceId, ballId, CartAction.PLUS))
                .then(cartService.changeQuantity(aliceId, racketId, CartAction.PLUS))
                .block();

        Long orderId = purchaseService.buy(aliceId, "alice").block();

        StepVerifier.create(orderService.getOrder(aliceId, orderId))
                .expectNext(new OrderDto(orderId, List.of(
                        new OrderItemDto(ballId, "Тестовый мяч", 1000, 2),
                        new OrderItemDto(racketId, "Тестовая ракетка", 4000, 1)), 2 * 1000 + 4000))
                .verifyComplete();
        StepVerifier.create(cartService.getCart(aliceId))
                .assertNext(cart -> assertThat(cart.items()).isEmpty())
                .verifyComplete();
    }

    @Test
    void buy_emptyCart_returnsError() {
        StepVerifier.create(purchaseService.buy(aliceId, "alice"))
                .expectError(EmptyCartException.class)
                .verify();
    }

    @Test
    void buy_severalTimes_ordersListedNewestFirst() {
        Long first = cartService.changeQuantity(aliceId, ballId, CartAction.PLUS).then(purchaseService.buy(aliceId, "alice")).block();
        Long second = cartService.changeQuantity(aliceId, racketId, CartAction.PLUS).then(purchaseService.buy(aliceId, "alice")).block();

        StepVerifier.create(orderService.findAll(aliceId).map(OrderDto::id))
                .expectNext(second, first)
                .verifyComplete();
    }

    @Test
    void buy_usesOnlyBuyersCartAndAccount() {
        cartService.changeQuantity(aliceId, ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(bobId, racketId, CartAction.PLUS))
                .block();

        Long orderId = purchaseService.buy(aliceId, "alice").block();

        assertThat(PAYMENT_SERVER.balance("alice")).isEqualTo(DEFAULT_BALANCE - 1000);
        assertThat(PAYMENT_SERVER.balance("bob")).isEqualTo(DEFAULT_BALANCE);
        StepVerifier.create(orderService.findAll(bobId)).verifyComplete();
        StepVerifier.create(orderService.getOrder(bobId, orderId))
                .expectError(NotFoundException.class)
                .verify();
        StepVerifier.create(cartService.getCart(bobId))
                .assertNext(cart -> assertThat(cart.items()).hasSize(1))
                .verifyComplete();
    }
}
