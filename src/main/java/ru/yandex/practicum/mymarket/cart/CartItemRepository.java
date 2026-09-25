package ru.yandex.practicum.mymarket.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByItemId(Long itemId);

    List<CartItem> findAllByItemIdIn(Collection<Long> itemIds);

    @EntityGraph(attributePaths = "item")
    List<CartItem> findAllByOrderByIdAsc();
}
