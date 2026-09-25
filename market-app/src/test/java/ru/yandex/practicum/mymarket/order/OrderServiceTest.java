package ru.yandex.practicum.mymarket.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.item.Item;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_savesOrderAndItemsWithOrderId() {
        Order order = new Order(LocalDateTime.now(), 200);
        OrderItem orderItem = new OrderItem(item(1L, 100), 2);
        when(orderRepository.save(order)).thenAnswer(invocation -> {
            order.setId(10L);
            return Mono.just(order);
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<OrderItem> items = invocation.getArgument(0);
            return Flux.fromIterable(items);
        });

        StepVerifier.create(orderService.create(order, List.of(orderItem)))
                .expectNext(10L)
                .verifyComplete();
        assertThat(orderItem.getOrderId()).isEqualTo(10L);
        verify(orderItemRepository).saveAll(List.of(orderItem));
    }

    @Test
    void getOrder_returnsOrderWithItems() {
        Order order = order(5L, 300);
        OrderItem orderItem = new OrderItem(item(1L, 100), 3);
        when(orderRepository.findById(5L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(5L)).thenReturn(Flux.just(orderItem));

        StepVerifier.create(orderService.getOrder(5L))
                .expectNext(new OrderDto(5L, List.of(new OrderItemDto(1L, "Товар 1", 100, 3)), 300))
                .verifyComplete();
    }

    @Test
    void getOrder_unknownId_returnsNotFound() {
        when(orderRepository.findById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(5L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void findAll_groupsItemsByOrder() {
        OrderItem first = new OrderItem(item(1L, 100), 1);
        first.setOrderId(1L);
        OrderItem second = new OrderItem(item(2L, 50), 2);
        second.setOrderId(2L);
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(Flux.just(order(2L, 100), order(1L, 100)));
        when(orderItemRepository.findAllByOrderIdInOrderByIdAsc(List.of(2L, 1L))).thenReturn(Flux.just(first, second));

        StepVerifier.create(orderService.findAll())
                .expectNext(new OrderDto(2L, List.of(new OrderItemDto(2L, "Товар 2", 50, 2)), 100))
                .expectNext(new OrderDto(1L, List.of(new OrderItemDto(1L, "Товар 1", 100, 1)), 100))
                .verifyComplete();
    }

    @Test
    void findAll_noOrders_doesNotQueryItems() {
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.findAll()).verifyComplete();
        verifyNoInteractions(orderItemRepository);
    }

    private static Order order(long id, long totalSum) {
        Order order = new Order(LocalDateTime.now(), totalSum);
        order.setId(id);
        return order;
    }

    private static Item item(long id, long price) {
        Item item = new Item("Товар " + id, "Описание " + id, "images/" + id + ".jpg", price);
        item.setId(id);
        return item;
    }
}
