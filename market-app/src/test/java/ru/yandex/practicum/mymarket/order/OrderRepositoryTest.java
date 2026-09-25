package ru.yandex.practicum.mymarket.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class OrderRepositoryTest extends RepositoryTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Item ball;
    private Item rope;

    @BeforeEach
    void setUp() {
        ball = itemRepository.save(new Item("Мяч", "", null, 100)).block();
        rope = itemRepository.save(new Item("Скакалка", "", null, 50)).block();
    }

    @Test
    void save_persistsOrderWithItems() {
        Long orderId = saveOrder(250, new OrderItem(ball, 2), new OrderItem(rope, 1));

        StepVerifier.create(orderRepository.findById(orderId).map(Order::getTotalSum))
                .expectNext(250L)
                .verifyComplete();
        StepVerifier.create(orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId).collectList())
                .assertNext(items -> assertThat(items)
                        .extracting(OrderItem::getItemId, OrderItem::getTitle, OrderItem::getPrice,
                                OrderItem::getQuantity)
                        .containsExactly(tuple(ball.getId(), "Мяч", 100L, 2), tuple(rope.getId(), "Скакалка", 50L, 1)))
                .verifyComplete();
    }

    @Test
    void orderItem_keepsPriceAtPurchaseTime() {
        Long orderId = saveOrder(100, new OrderItem(ball, 1));
        ball.setPrice(500);
        itemRepository.save(ball).block();

        StepVerifier.create(orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId).map(OrderItem::getPrice))
                .expectNext(100L)
                .verifyComplete();
    }

    @Test
    void findAllByOrderByIdDesc_returnsNewestFirst() {
        Long older = saveOrder(100, new OrderItem(ball, 1));
        Long newer = saveOrder(200, new OrderItem(ball, 2));

        StepVerifier.create(orderRepository.findAllByOrderByIdDesc().map(Order::getId))
                .expectNext(newer, older)
                .verifyComplete();
    }

    @Test
    void findAllByOrderIdIn_returnsItemsOfRequestedOrders() {
        Long first = saveOrder(100, new OrderItem(ball, 1));
        Long second = saveOrder(50, new OrderItem(rope, 1));
        Long third = saveOrder(200, new OrderItem(ball, 2));

        StepVerifier.create(orderItemRepository.findAllByOrderIdInOrderByIdAsc(List.of(first, third))
                        .map(OrderItem::getOrderId))
                .expectNext(first, third)
                .verifyComplete();
        assertThat(second).isNotNull();
    }

    private Long saveOrder(long totalSum, OrderItem... items) {
        return orderRepository.save(new Order(LocalDateTime.now(), totalSum))
                .flatMap(order -> Flux.just(items)
                        .doOnNext(item -> item.setOrderId(order.getId()))
                        .concatMap(orderItemRepository::save)
                        .then()
                        .thenReturn(order.getId()))
                .block();
    }
}
