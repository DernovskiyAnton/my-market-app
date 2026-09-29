package ru.yandex.practicum.mymarket.order;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {

    Flux<Order> findAllByUserIdOrderByIdDesc(Long userId);

    Mono<Order> findByIdAndUserId(Long id, Long userId);
}
