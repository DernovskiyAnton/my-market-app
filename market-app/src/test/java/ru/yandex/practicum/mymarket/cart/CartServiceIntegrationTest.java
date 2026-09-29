package ru.yandex.practicum.mymarket.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.item.ItemService;
import ru.yandex.practicum.mymarket.item.SortType;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class CartServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemService itemService;

    private long aliceId;
    private long bobId;
    private long ballId;
    private long ropeId;

    @BeforeEach
    void setUp() {
        aliceId = user("alice").getId();
        bobId = user("bob").getId();
        ballId = createItem("Тестовый мяч", 1000).getId();
        ropeId = createItem("Тестовая скакалка", 300).getId();
    }

    @Test
    void changeQuantity_updatesCartAndCatalogCounts() {
        cartService.changeQuantity(aliceId, ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(aliceId, ballId, CartAction.PLUS))
                .then(cartService.changeQuantity(aliceId, ropeId, CartAction.PLUS))
                .block();

        StepVerifier.create(cartService.getCart(aliceId))
                .assertNext(cart -> {
                    assertThat(cart.items()).extracting(ItemDto::id, ItemDto::count)
                            .containsExactly(tuple(ballId, 2), tuple(ropeId, 1));
                    assertThat(cart.total()).isEqualTo(2 * 1000 + 300);
                })
                .verifyComplete();
        StepVerifier.create(itemService.getItem(aliceId, ballId).map(ItemDto::count))
                .expectNext(2)
                .verifyComplete();
        StepVerifier.create(itemService.findItems(aliceId, "Тестовая скакалка", SortType.NO, 1, 5))
                .assertNext(page -> assertThat(page.getContent()).singleElement()
                        .extracting(ItemDto::count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void changeQuantity_minusAndDelete_removeItemsFromCart() {
        cartService.changeQuantity(aliceId, ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(aliceId, ropeId, CartAction.PLUS))
                .then(cartService.changeQuantity(aliceId, ropeId, CartAction.PLUS))
                .then(cartService.changeQuantity(aliceId, ballId, CartAction.MINUS))
                .then(cartService.changeQuantity(aliceId, ropeId, CartAction.DELETE))
                .block();

        StepVerifier.create(cartService.getCart(aliceId))
                .assertNext(cart -> {
                    assertThat(cart.items()).isEmpty();
                    assertThat(cart.total()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void carts_ofDifferentUsers_areIndependent() {
        cartService.changeQuantity(aliceId, ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(bobId, ropeId, CartAction.PLUS))
                .then(cartService.changeQuantity(bobId, ropeId, CartAction.PLUS))
                .block();

        StepVerifier.create(cartService.getCart(aliceId))
                .assertNext(cart -> assertThat(cart.items()).extracting(ItemDto::id, ItemDto::count)
                        .containsExactly(tuple(ballId, 1)))
                .verifyComplete();
        StepVerifier.create(cartService.getCart(bobId))
                .assertNext(cart -> assertThat(cart.items()).extracting(ItemDto::id, ItemDto::count)
                        .containsExactly(tuple(ropeId, 2)))
                .verifyComplete();

        cartService.clear(bobId).block();

        StepVerifier.create(cartService.getCart(aliceId))
                .assertNext(cart -> assertThat(cart.items()).hasSize(1))
                .verifyComplete();
        StepVerifier.create(itemService.findItems(bobId, "Тестовый мяч", SortType.NO, 1, 5))
                .assertNext(page -> assertThat(page.getContent()).singleElement()
                        .extracting(ItemDto::count).isEqualTo(0))
                .verifyComplete();
    }
}
