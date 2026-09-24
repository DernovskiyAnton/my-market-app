package ru.yandex.practicum.mymarket.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.item.Item;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_savesOrderAndReturnsId() {
        Order order = new Order(LocalDateTime.now());
        order.addItem(new OrderItem(item(1L, 100), 2));
        when(orderRepository.save(order)).thenAnswer(invocation -> {
            order.setId(10L);
            return order;
        });

        assertThat(orderService.create(order)).isEqualTo(10L);
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
