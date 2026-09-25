package ru.yandex.practicum.mymarket.cart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemDto;
import ru.yandex.practicum.mymarket.item.ItemRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cartService.changeQuantity(1L, CartAction.PLUS)).verifyComplete();

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getItemId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
    }

    @Test
    void changeQuantity_plus_incrementsExistingItem() {
        CartItem cartItem = new CartItem(1L, 2);
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.changeQuantity(1L, CartAction.PLUS)).verifyComplete();

        assertThat(cartItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void changeQuantity_minus_decrementsQuantity() {
        CartItem cartItem = new CartItem(1L, 2);
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.changeQuantity(1L, CartAction.MINUS)).verifyComplete();

        assertThat(cartItem.getQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void changeQuantity_minus_removesItemWhenLastOne() {
        CartItem cartItem = new CartItem(1L, 1);
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(1L, CartAction.MINUS)).verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void changeQuantity_minus_doesNothingWhenItemNotInCart() {
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(1L, CartAction.MINUS)).verifyComplete();

        verify(cartItemRepository, never()).delete(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void changeQuantity_delete_removesItemFromCart() {
        CartItem cartItem = new CartItem(1L, 5);
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(1L, CartAction.DELETE)).verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void changeQuantity_unknownItem_returnsNotFound() {
        when(itemRepository.existsById(42L)).thenReturn(Mono.just(false));

        StepVerifier.create(cartService.changeQuantity(42L, CartAction.PLUS))
                .expectError(NotFoundException.class)
                .verify();
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getCartLines_joinsCartItemsWithItemsInCartOrder() {
        Item ball = item(1L, 100);
        Item rope = item(2L, 50);
        when(cartItemRepository.findAllByOrderByIdAsc())
                .thenReturn(Flux.just(new CartItem(2L, 3), new CartItem(1L, 2)));
        when(itemRepository.findAllById(List.of(2L, 1L))).thenReturn(Flux.just(ball, rope));

        StepVerifier.create(cartService.getCartLines())
                .expectNext(new CartLine(rope, 3), new CartLine(ball, 2))
                .verifyComplete();
    }

    @Test
    void getCart_returnsItemsAndTotal() {
        when(cartItemRepository.findAllByOrderByIdAsc())
                .thenReturn(Flux.just(new CartItem(1L, 2), new CartItem(2L, 3)));
        when(itemRepository.findAllById(List.of(1L, 2L))).thenReturn(Flux.just(item(1L, 100), item(2L, 50)));

        StepVerifier.create(cartService.getCart())
                .assertNext(cart -> {
                    assertThat(cart.items()).extracting(ItemDto::id, ItemDto::count)
                            .containsExactly(tuple(1L, 2),
                                    tuple(2L, 3));
                    assertThat(cart.total()).isEqualTo(350);
                })
                .verifyComplete();
    }

    @Test
    void getCart_empty_returnsZeroTotal() {
        when(cartItemRepository.findAllByOrderByIdAsc()).thenReturn(Flux.empty());

        StepVerifier.create(cartService.getCart())
                .expectNext(new CartDto(List.of(), 0))
                .verifyComplete();
        verifyNoInteractions(itemRepository);
    }

    @Test
    void getQuantities_returnsMapByItemId() {
        when(cartItemRepository.findAllByItemIdIn(List.of(1L, 2L))).thenReturn(Flux.just(new CartItem(1L, 4)));

        StepVerifier.create(cartService.getQuantities(List.of(1L, 2L)))
                .expectNext(Map.of(1L, 4))
                .verifyComplete();
    }

    @Test
    void getQuantities_emptyIds_doesNotQueryRepository() {
        StepVerifier.create(cartService.getQuantities(List.of()))
                .expectNext(Map.of())
                .verifyComplete();
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getQuantity_returnsZeroWhenNotInCart() {
        when(cartItemRepository.findByItemId(7L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.getQuantity(7L))
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    void clear_deletesAllCartItems() {
        when(cartItemRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(cartService.clear()).verifyComplete();

        verify(cartItemRepository).deleteAll();
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
