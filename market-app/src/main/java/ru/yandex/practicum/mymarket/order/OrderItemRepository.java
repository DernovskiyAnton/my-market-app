package ru.yandex.practicum.mymarket.order;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Collection;

@Repository
public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {

    Flux<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);

    Flux<OrderItem> findAllByOrderIdInOrderByIdAsc(Collection<Long> orderIds);
}
