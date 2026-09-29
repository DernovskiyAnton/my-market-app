package ru.yandex.practicum.mymarket.cart;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    Mono<CartItem> findByItemId(Long itemId);

    Flux<CartItem> findAllByItemIdIn(Collection<Long> itemIds);

    Flux<CartItem> findAllByOrderByIdAsc();
}
