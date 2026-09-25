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

    private long ballId;
    private long ropeId;

    @BeforeEach
    void setUp() {
        ballId = createItem("Тестовый мяч", 1000).getId();
        ropeId = createItem("Тестовая скакалка", 300).getId();
    }

    @Test
    void changeQuantity_updatesCartAndCatalogCounts() {
        cartService.changeQuantity(ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(ballId, CartAction.PLUS))
                .then(cartService.changeQuantity(ropeId, CartAction.PLUS))
                .block();

        StepVerifier.create(cartService.getCart())
                .assertNext(cart -> {
                    assertThat(cart.items()).extracting(ItemDto::id, ItemDto::count)
                            .containsExactly(tuple(ballId, 2), tuple(ropeId, 1));
                    assertThat(cart.total()).isEqualTo(2 * 1000 + 300);
                })
                .verifyComplete();
        StepVerifier.create(itemService.getItem(ballId).map(ItemDto::count))
                .expectNext(2)
                .verifyComplete();
        StepVerifier.create(itemService.findItems("Тестовая скакалка", SortType.NO, 1, 5))
                .assertNext(page -> assertThat(page.getContent()).singleElement()
                        .extracting(ItemDto::count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void changeQuantity_minusAndDelete_removeItemsFromCart() {
        cartService.changeQuantity(ballId, CartAction.PLUS)
                .then(cartService.changeQuantity(ropeId, CartAction.PLUS))
                .then(cartService.changeQuantity(ropeId, CartAction.PLUS))
                .then(cartService.changeQuantity(ballId, CartAction.MINUS))
                .then(cartService.changeQuantity(ropeId, CartAction.DELETE))
                .block();

        StepVerifier.create(cartService.getCart())
                .assertNext(cart -> {
                    assertThat(cart.items()).isEmpty();
                    assertThat(cart.total()).isZero();
                })
                .verifyComplete();
    }
}
