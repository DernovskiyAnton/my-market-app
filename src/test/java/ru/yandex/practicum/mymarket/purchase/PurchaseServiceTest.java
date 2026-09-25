package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cart.CartLine;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.EmptyCartException;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.order.Order;
import ru.yandex.practicum.mymarket.order.OrderItem;
import ru.yandex.practicum.mymarket.order.OrderService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(cartService, orderService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buy_createsOrderFromCartAndClearsCart() {
        when(cartService.getCartLines()).thenReturn(Flux.just(
                new CartLine(item(1L, 100), 2),
                new CartLine(item(2L, 30), 1)));
        when(orderService.create(any(Order.class), anyList())).thenReturn(Mono.just(10L));
        when(cartService.clear()).thenReturn(Mono.empty());

        StepVerifier.create(purchaseService.buy())
                .expectNext(10L)
                .verifyComplete();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        InOrder inOrder = inOrder(orderService, cartService);
        inOrder.verify(orderService).create(orderCaptor.capture(), itemsCaptor.capture());
        inOrder.verify(cartService).clear();
        assertThat(orderCaptor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(orderCaptor.getValue().getTotalSum()).isEqualTo(230);
        assertThat(itemsCaptor.getValue())
                .extracting(OrderItem::getItemId, OrderItem::getTitle, OrderItem::getPrice, OrderItem::getQuantity)
                .containsExactly(tuple(1L, "Товар 1", 100L, 2), tuple(2L, "Товар 2", 30L, 1));
    }

    @Test
    void buy_emptyCart_returnsError() {
        when(cartService.getCartLines()).thenReturn(Flux.empty());

        StepVerifier.create(purchaseService.buy())
                .expectError(EmptyCartException.class)
                .verify();
        verify(orderService, never()).create(any(), anyList());
        verify(cartService, never()).clear();
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
