package ru.yandex.practicum.mymarket.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import java.util.List;

class CartItemRepositoryTest extends RepositoryTestBase {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Long firstId;
    private Long secondId;
    private Long thirdId;

    @BeforeEach
    void setUp() {
        firstId = saveItem("Первый", 10);
        secondId = saveItem("Второй", 20);
        thirdId = saveItem("Третий", 30);
    }

    @Test
    void findByItemId_returnsCartItem() {
        cartItemRepository.save(new CartItem(firstId, 3)).block();

        StepVerifier.create(cartItemRepository.findByItemId(firstId).map(CartItem::getQuantity))
                .expectNext(3)
                .verifyComplete();
        StepVerifier.create(cartItemRepository.findByItemId(secondId))
                .verifyComplete();
    }

    @Test
    void findAllByItemIdIn_returnsOnlyRequestedItems() {
        cartItemRepository.save(new CartItem(firstId, 1)).block();
        cartItemRepository.save(new CartItem(thirdId, 2)).block();

        StepVerifier.create(cartItemRepository.findAllByItemIdIn(List.of(firstId, secondId)).map(CartItem::getItemId))
                .expectNext(firstId)
                .verifyComplete();
    }

    @Test
    void findAllByOrderByIdAsc_returnsItemsInInsertionOrder() {
        cartItemRepository.save(new CartItem(secondId, 1)).block();
        cartItemRepository.save(new CartItem(firstId, 2)).block();

        StepVerifier.create(cartItemRepository.findAllByOrderByIdAsc().map(CartItem::getItemId))
                .expectNext(secondId, firstId)
                .verifyComplete();
    }

    @Test
    void save_sameItemTwice_violatesUniqueConstraint() {
        cartItemRepository.save(new CartItem(firstId, 1)).block();

        StepVerifier.create(cartItemRepository.save(new CartItem(firstId, 2)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void save_unknownItem_violatesForeignKey() {
        StepVerifier.create(cartItemRepository.save(new CartItem(-1L, 1)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    private Long saveItem(String title, long price) {
        return itemRepository.save(new Item(title, "", null, price)).map(Item::getId).block();
    }
}
