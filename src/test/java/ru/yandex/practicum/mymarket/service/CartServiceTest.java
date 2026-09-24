package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void changeQuantity_plus_addsNewItemToCart() {
        Item item = item(1L, 100);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        cartService.changeQuantity(1L, CartAction.PLUS);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
    }

    @Test
    void changeQuantity_plus_incrementsExistingItem() {
        Item item = item(1L, 100);
        CartItem cartItem = new CartItem(item, 2);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.changeQuantity(1L, CartAction.PLUS);

        assertThat(cartItem.getQuantity()).isEqualTo(3);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void changeQuantity_minus_decrementsQuantity() {
        Item item = item(1L, 100);
        CartItem cartItem = new CartItem(item, 2);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.changeQuantity(1L, CartAction.MINUS);

        assertThat(cartItem.getQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void changeQuantity_minus_removesItemWhenLastOne() {
        Item item = item(1L, 100);
        CartItem cartItem = new CartItem(item, 1);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.changeQuantity(1L, CartAction.MINUS);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void changeQuantity_minus_doesNothingWhenItemNotInCart() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, 100)));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        cartService.changeQuantity(1L, CartAction.MINUS);

        verify(cartItemRepository, never()).delete(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void changeQuantity_delete_removesItemFromCart() {
        Item item = item(1L, 100);
        CartItem cartItem = new CartItem(item, 5);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem));

        cartService.changeQuantity(1L, CartAction.DELETE);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void changeQuantity_unknownItem_throwsNotFound() {
        when(itemRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.changeQuantity(42L, CartAction.PLUS))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getCart_returnsItemsAndTotal() {
        when(cartItemRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                new CartItem(item(1L, 100), 2),
                new CartItem(item(2L, 50), 3)));

        CartDto cart = cartService.getCart();

        assertThat(cart.items()).extracting("id", "count").containsExactly(
                tuple(1L, 2),
                tuple(2L, 3));
        assertThat(cart.total()).isEqualTo(350);
    }

    @Test
    void getCart_empty_returnsZeroTotal() {
        when(cartItemRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        CartDto cart = cartService.getCart();

        assertThat(cart.items()).isEmpty();
        assertThat(cart.total()).isZero();
    }

    @Test
    void getQuantities_returnsMapByItemId() {
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(new CartItem(item(1L, 100), 4)));

        Map<Long, Integer> quantities = cartService.getQuantities(List.of(1L, 2L));

        assertThat(quantities).containsExactly(Map.entry(1L, 4));
    }

    @Test
    void getQuantities_emptyIds_doesNotQueryRepository() {
        assertThat(cartService.getQuantities(List.of())).isEmpty();
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getQuantity_returnsZeroWhenNotInCart() {
        when(cartItemRepository.findByItemId(7L)).thenReturn(Optional.empty());

        assertThat(cartService.getQuantity(7L)).isZero();
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
