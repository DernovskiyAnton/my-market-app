package ru.yandex.practicum.mymarket.cart;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartItemRepositoryTest extends RepositoryTestBase {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void findByItemId_returnsCartItem() {
        Item item = entityManager.persist(new Item("Товар", "Описание", null, 100));
        entityManager.persist(new CartItem(item, 3));
        entityManager.clear();

        assertThat(cartItemRepository.findByItemId(item.getId())).get()
                .extracting(CartItem::getQuantity).isEqualTo(3);
        assertThat(cartItemRepository.findByItemId(-1L)).isEmpty();
    }

    @Test
    void findAllByItemIdIn_returnsOnlyRequestedItems() {
        Item first = entityManager.persist(new Item("Первый", "", null, 10));
        Item second = entityManager.persist(new Item("Второй", "", null, 20));
        Item third = entityManager.persist(new Item("Третий", "", null, 30));
        entityManager.persist(new CartItem(first, 1));
        entityManager.persist(new CartItem(third, 2));
        entityManager.clear();

        List<CartItem> cartItems = cartItemRepository.findAllByItemIdIn(
                List.of(first.getId(), second.getId()));

        assertThat(cartItems).singleElement()
                .extracting(cartItem -> cartItem.getItem().getId()).isEqualTo(first.getId());
    }

    @Test
    void findAllByOrderByIdAsc_fetchesItems() {
        Item first = entityManager.persist(new Item("Первый", "", null, 10));
        Item second = entityManager.persist(new Item("Второй", "", null, 20));
        entityManager.persist(new CartItem(first, 1));
        entityManager.persist(new CartItem(second, 2));
        entityManager.clear();

        List<CartItem> cartItems = cartItemRepository.findAllByOrderByIdAsc();

        assertThat(cartItems).extracting(cartItem -> cartItem.getItem().getTitle())
                .containsExactly("Первый", "Второй");
    }

    @Test
    void save_sameItemTwice_violatesUniqueConstraint() {
        Item item = entityManager.persist(new Item("Товар", "", null, 10));
        entityManager.persist(new CartItem(item, 1));

        assertThatThrownBy(() -> {
            entityManager.persist(new CartItem(item, 2));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
