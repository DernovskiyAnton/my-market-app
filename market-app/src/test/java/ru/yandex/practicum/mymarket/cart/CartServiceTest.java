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
import ru.yandex.practicum.mymarket.item.ItemCache;
import ru.yandex.practicum.mymarket.item.ItemCard;
import ru.yandex.practicum.mymarket.item.ItemDto;

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

    private static final long USER_ID = 1L;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemCache itemCache;

    @InjectMocks
    private CartService cartService;

    @Test
    void changeQuantity_plus_addsNewItemToCart() {
        when(itemCache.getCard(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cartService.changeQuantity(USER_ID, 1L, CartAction.PLUS)).verifyComplete();

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getItemId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
    }

    @Test
    void changeQuantity_plus_incrementsExistingItem() {
        CartItem cartItem = new CartItem(USER_ID, 1L, 2);
        when(itemCache.getCard(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.changeQuantity(USER_ID, 1L, CartAction.PLUS)).verifyComplete();

        assertThat(cartItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void changeQuantity_minus_decrementsQuantity() {
        CartItem cartItem = new CartItem(USER_ID, 1L, 2);
        when(itemCache.getCard(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(cartItem)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.changeQuantity(USER_ID, 1L, CartAction.MINUS)).verifyComplete();

        assertThat(cartItem.getQuantity()).isEqualTo(1);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void changeQuantity_minus_removesItemWhenLastOne() {
        CartItem cartItem = new CartItem(USER_ID, 1L, 1);
        when(itemCache.getCard(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(USER_ID, 1L, CartAction.MINUS)).verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void changeQuantity_minus_doesNothingWhenItemNotInCart() {
        when(itemCache.getCard(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(USER_ID, 1L, CartAction.MINUS)).verifyComplete();

        verify(cartItemRepository, never()).delete(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void changeQuantity_delete_removesItemFromCart() {
        CartItem cartItem = new CartItem(USER_ID, 1L, 5);
        when(itemCache.getCard(1L)).thenReturn(Mono.just(item(1L, 100)));
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(USER_ID, 1L, CartAction.DELETE)).verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void changeQuantity_unknownItem_returnsNotFound() {
        when(itemCache.getCard(42L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeQuantity(USER_ID, 42L, CartAction.PLUS))
                .expectError(NotFoundException.class)
                .verify();
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getCartLines_joinsCartItemsWithItemsInCartOrder() {
        ItemCard ball = item(1L, 100);
        ItemCard rope = item(2L, 50);
        when(cartItemRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                .thenReturn(Flux.just(new CartItem(USER_ID, 2L, 3), new CartItem(USER_ID, 1L, 2)));
        when(itemCache.getCards(List.of(2L, 1L))).thenReturn(Mono.just(Map.of(1L, ball, 2L, rope)));

        StepVerifier.create(cartService.getCartLines(USER_ID))
                .expectNext(new CartLine(rope, 3), new CartLine(ball, 2))
                .verifyComplete();
    }

    @Test
    void getCartLines_skipsItemsMissingInCatalog() {
        when(cartItemRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                .thenReturn(Flux.just(new CartItem(USER_ID, 1L, 1), new CartItem(USER_ID, 9L, 1)));
        when(itemCache.getCards(List.of(1L, 9L))).thenReturn(Mono.just(Map.of(1L, item(1L, 100))));

        StepVerifier.create(cartService.getCartLines(USER_ID))
                .expectNext(new CartLine(item(1L, 100), 1))
                .verifyComplete();
    }

    @Test
    void getCart_returnsItemsAndTotal() {
        when(cartItemRepository.findAllByUserIdOrderByIdAsc(USER_ID))
                .thenReturn(Flux.just(new CartItem(USER_ID, 1L, 2), new CartItem(USER_ID, 2L, 3)));
        when(itemCache.getCards(List.of(1L, 2L))).thenReturn(Mono.just(Map.of(1L, item(1L, 100), 2L, item(2L, 50))));

        StepVerifier.create(cartService.getCart(USER_ID))
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
        when(cartItemRepository.findAllByUserIdOrderByIdAsc(USER_ID)).thenReturn(Flux.empty());

        StepVerifier.create(cartService.getCart(USER_ID))
                .expectNext(new CartDto(List.of(), 0))
                .verifyComplete();
        verifyNoInteractions(itemCache);
    }

    @Test
    void getQuantities_returnsMapByItemId() {
        when(cartItemRepository.findAllByUserIdAndItemIdIn(USER_ID, List.of(1L, 2L))).thenReturn(Flux.just(new CartItem(USER_ID, 1L, 4)));

        StepVerifier.create(cartService.getQuantities(USER_ID, List.of(1L, 2L)))
                .expectNext(Map.of(1L, 4))
                .verifyComplete();
    }

    @Test
    void getQuantities_emptyIds_doesNotQueryRepository() {
        StepVerifier.create(cartService.getQuantities(USER_ID, List.of()))
                .expectNext(Map.of())
                .verifyComplete();
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void getQuantity_returnsZeroWhenNotInCart() {
        when(cartItemRepository.findByUserIdAndItemId(USER_ID, 7L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.getQuantity(USER_ID, 7L))
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    void clear_deletesAllCartItems() {
        when(cartItemRepository.deleteAllByUserId(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.clear(USER_ID)).verifyComplete();

        verify(cartItemRepository).deleteAllByUserId(USER_ID);
    }

    private static ItemCard item(long id, long price) {
        return new ItemCard(id, "Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
    }

    @Test
    void anonymousUser_hasNoQuantitiesAndDoesNotQueryCart() {
        StepVerifier.create(cartService.getQuantities(null, List.of(1L, 2L)))
                .expectNext(Map.of())
                .verifyComplete();
        StepVerifier.create(cartService.getQuantity(null, 1L))
                .expectNext(0)
                .verifyComplete();
        verifyNoInteractions(cartItemRepository);
    }
}
