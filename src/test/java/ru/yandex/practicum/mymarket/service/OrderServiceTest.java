package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.exception.EmptyCartException;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        orderService = new OrderService(orderRepository, cartItemRepository, cartService, clock);
    }

    @Test
    void createOrderFromCart_createsOrderAndClearsCart() {
        when(cartItemRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                new CartItem(item(1L, 100), 2),
                new CartItem(item(2L, 30), 1)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });

        long orderId = orderService.createOrderFromCart();

        assertThat(orderId).isEqualTo(10L);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order order = captor.getValue();
        assertThat(order.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(order.getTotalSum()).isEqualTo(230);
        assertThat(order.getItems()).extracting(OrderItem::getTitle, OrderItem::getPrice, OrderItem::getQuantity)
                .containsExactly(
                        tuple("Товар 1", 100L, 2),
                        tuple("Товар 2", 30L, 1));
        assertThat(order.getItems()).allMatch(orderItem -> orderItem.getOrder() == order);
        verify(cartService).clear();
    }

    @Test
    void createOrderFromCart_emptyCart_throwsException() {
        when(cartItemRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrderFromCart()).isInstanceOf(EmptyCartException.class);
        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clear();
    }

    @Test
    void getOrder_returnsOrderDto() {
        Order order = new Order(LocalDateTime.now());
        order.setId(5L);
        order.addItem(new OrderItem(item(1L, 100), 3));
        when(orderRepository.findWithItemsById(5L)).thenReturn(Optional.of(order));

        OrderDto dto = orderService.getOrder(5L);

        assertThat(dto).isEqualTo(new OrderDto(5L, List.of(new OrderItemDto(1L, "Товар 1", 100, 3)), 300));
    }

    @Test
    void getOrder_unknownId_throwsNotFound() {
        when(orderRepository.findWithItemsById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAll_mapsAllOrders() {
        Order first = new Order(LocalDateTime.now());
        first.setId(2L);
        Order second = new Order(LocalDateTime.now());
        second.setId(1L);
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(List.of(first, second));

        assertThat(orderService.findAll()).extracting(OrderDto::id).containsExactly(2L, 1L);
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
