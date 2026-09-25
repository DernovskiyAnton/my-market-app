package ru.yandex.practicum.mymarket.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.cart.CartItem;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
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
    void buy_createsOrderFromCartAndClearsCart() {
        when(cartService.getCartItems()).thenReturn(List.of(
                new CartItem(item(1L, 100), 2),
                new CartItem(item(2L, 30), 1)));
        when(orderService.create(any(Order.class))).thenReturn(10L);

        long orderId = purchaseService.buy();

        assertThat(orderId).isEqualTo(10L);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        InOrder inOrder = inOrder(orderService, cartService);
        inOrder.verify(orderService).create(captor.capture());
        inOrder.verify(cartService).clear();
        Order order = captor.getValue();
        assertThat(order.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(order.getTotalSum()).isEqualTo(230);
        assertThat(order.getItems()).extracting(OrderItem::getTitle, OrderItem::getPrice, OrderItem::getQuantity)
                .containsExactly(
                        tuple("Товар 1", 100L, 2),
                        tuple("Товар 2", 30L, 1));
        assertThat(order.getItems()).allMatch(orderItem -> orderItem.getOrder() == order);
    }

    @Test
    void buy_emptyCart_throwsException() {
        when(cartService.getCartItems()).thenReturn(List.of());

        assertThatThrownBy(() -> purchaseService.buy()).isInstanceOf(EmptyCartException.class);
        verify(orderService, never()).create(any());
        verify(cartService, never()).clear();
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
