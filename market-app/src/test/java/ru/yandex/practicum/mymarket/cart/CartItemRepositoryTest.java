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

    private Long alice;
    private Long bob;
    private Long firstId;
    private Long secondId;
    private Long thirdId;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        firstId = saveItem("Первый", 10);
        secondId = saveItem("Второй", 20);
        thirdId = saveItem("Третий", 30);
    }

    @Test
    void findByUserIdAndItemId_returnsOnlyUsersCartItem() {
        save(alice, firstId, 3);
        save(bob, firstId, 7);

        StepVerifier.create(cartItemRepository.findByUserIdAndItemId(alice, firstId).map(CartItem::getQuantity))
                .expectNext(3)
                .verifyComplete();
        StepVerifier.create(cartItemRepository.findByUserIdAndItemId(bob, firstId).map(CartItem::getQuantity))
                .expectNext(7)
                .verifyComplete();
        StepVerifier.create(cartItemRepository.findByUserIdAndItemId(alice, secondId))
                .verifyComplete();
    }

    @Test
    void findAllByUserIdAndItemIdIn_returnsOnlyRequestedItemsOfUser() {
        save(alice, firstId, 1);
        save(alice, thirdId, 2);
        save(bob, secondId, 1);

        StepVerifier.create(cartItemRepository.findAllByUserIdAndItemIdIn(alice, List.of(firstId, secondId))
                        .map(CartItem::getItemId))
                .expectNext(firstId)
                .verifyComplete();
    }

    @Test
    void findAllByUserIdOrderByIdAsc_returnsUsersItemsInInsertionOrder() {
        save(alice, secondId, 1);
        save(bob, thirdId, 1);
        save(alice, firstId, 2);

        StepVerifier.create(cartItemRepository.findAllByUserIdOrderByIdAsc(alice).map(CartItem::getItemId))
                .expectNext(secondId, firstId)
                .verifyComplete();
    }

    @Test
    void deleteAllByUserId_clearsOnlyUsersCart() {
        save(alice, firstId, 1);
        save(alice, secondId, 1);
        save(bob, firstId, 1);

        StepVerifier.create(cartItemRepository.deleteAllByUserId(alice)).verifyComplete();

        StepVerifier.create(cartItemRepository.findAllByUserIdOrderByIdAsc(alice)).verifyComplete();
        StepVerifier.create(cartItemRepository.findAllByUserIdOrderByIdAsc(bob).map(CartItem::getItemId))
                .expectNext(firstId)
                .verifyComplete();
    }

    @Test
    void save_sameItemTwiceForSameUser_violatesUniqueConstraint() {
        save(alice, firstId, 1);

        StepVerifier.create(cartItemRepository.save(new CartItem(alice, firstId, 2)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void save_unknownItemOrUser_violatesForeignKey() {
        StepVerifier.create(cartItemRepository.save(new CartItem(alice, -1L, 1)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
        StepVerifier.create(cartItemRepository.save(new CartItem(-1L, firstId, 1)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    private void save(Long userId, Long itemId, int quantity) {
        cartItemRepository.save(new CartItem(userId, itemId, quantity)).block();
    }

    private Long saveItem(String title, long price) {
        return itemRepository.save(new Item(title, "", null, price)).map(Item::getId).block();
    }
}
