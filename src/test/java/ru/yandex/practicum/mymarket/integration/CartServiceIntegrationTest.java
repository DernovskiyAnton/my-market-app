package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class CartServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemService itemService;

    @Test
    void changeQuantity_updatesCartAndCatalogCounts() {
        cartService.changeQuantity(1L, CartAction.PLUS);
        cartService.changeQuantity(1L, CartAction.PLUS);
        cartService.changeQuantity(4L, CartAction.PLUS);

        CartDto cart = cartService.getCart();
        assertThat(cart.items()).extracting(ItemDto::id, ItemDto::count)
                .containsExactly(tuple(1L, 2), tuple(4L, 1));
        assertThat(cart.total()).isEqualTo(2 * 2500 + 600);
        assertThat(itemService.getItem(1L).count()).isEqualTo(2);
        assertThat(itemService.findItems("", SortType.NO, 1, 5).getContent())
                .filteredOn(item -> item.id() == 4L)
                .singleElement().extracting(ItemDto::count).isEqualTo(1);
    }

    @Test
    void changeQuantity_minusAndDelete_removeItemsFromCart() {
        cartService.changeQuantity(1L, CartAction.PLUS);
        cartService.changeQuantity(2L, CartAction.PLUS);
        cartService.changeQuantity(2L, CartAction.PLUS);

        cartService.changeQuantity(1L, CartAction.MINUS);
        cartService.changeQuantity(2L, CartAction.DELETE);

        assertThat(cartService.getCart().items()).isEmpty();
        assertThat(cartService.getCart().total()).isZero();
    }
}
