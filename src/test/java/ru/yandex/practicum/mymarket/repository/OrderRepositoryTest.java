package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class OrderRepositoryTest extends RepositoryTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void save_cascadesOrderItems() {
        Item ball = entityManager.persist(new Item("Мяч", "", null, 100));
        Item rope = entityManager.persist(new Item("Скакалка", "", null, 50));
        Order order = new Order(LocalDateTime.now());
        order.addItem(new OrderItem(ball, 2));
        order.addItem(new OrderItem(rope, 1));

        Long id = orderRepository.save(order).getId();
        entityManager.flush();
        entityManager.clear();

        Order found = orderRepository.findWithItemsById(id).orElseThrow();
        assertThat(found.getTotalSum()).isEqualTo(250);
        assertThat(found.getItems())
                .extracting(OrderItem::getTitle, OrderItem::getPrice, OrderItem::getQuantity)
                .containsExactly(tuple("Мяч", 100L, 2), tuple("Скакалка", 50L, 1));
    }

    @Test
    void orderItem_keepsPriceAtPurchaseTime() {
        Item item = entityManager.persist(new Item("Мяч", "", null, 100));
        Order order = new Order(LocalDateTime.now());
        order.addItem(new OrderItem(item, 1));
        Long id = orderRepository.save(order).getId();
        item.setPrice(500);
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findWithItemsById(id).orElseThrow().getItems())
                .singleElement().extracting(OrderItem::getPrice).isEqualTo(100L);
    }

    @Test
    void findAllByOrderByIdDesc_returnsNewestFirst() {
        Item item = entityManager.persist(new Item("Мяч", "", null, 100));
        Order older = new Order(LocalDateTime.now());
        older.addItem(new OrderItem(item, 1));
        Order newer = new Order(LocalDateTime.now());
        newer.addItem(new OrderItem(item, 2));
        orderRepository.save(older);
        orderRepository.save(newer);
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findAllByOrderByIdDesc())
                .extracting(Order::getId)
                .containsExactly(newer.getId(), older.getId());
    }
}
