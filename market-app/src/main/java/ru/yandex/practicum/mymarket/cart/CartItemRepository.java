package ru.yandex.practicum.mymarket.cart;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    Mono<CartItem> findByUserIdAndItemId(Long userId, Long itemId);

    Flux<CartItem> findAllByUserIdAndItemIdIn(Long userId, Collection<Long> itemIds);

    Flux<CartItem> findAllByUserIdOrderByIdAsc(Long userId);

    @Modifying
    @Query("DELETE FROM cart_items WHERE user_id = :userId")
    Mono<Void> deleteAllByUserId(Long userId);
}
