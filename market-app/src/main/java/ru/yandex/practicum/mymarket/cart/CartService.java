package ru.yandex.practicum.mymarket.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.common.NotFoundException;
import ru.yandex.practicum.mymarket.item.ItemCache;
import ru.yandex.practicum.mymarket.item.ItemMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemCache itemCache;

    public Flux<CartLine> getCartLines() {
        return cartItemRepository.findAllByOrderByIdAsc()
                .collectList()
                .flatMapMany(this::toCartLines);
    }

    public Mono<CartDto> getCart() {
        return getCartLines()
                .map(line -> ItemMapper.toDto(line.item(), line.quantity()))
                .collectList()
                .map(items -> new CartDto(items, items.stream().mapToLong(item -> item.price() * item.count()).sum()));
    }

    public Mono<Map<Long, Integer>> getQuantities(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        return cartItemRepository.findAllByItemIdIn(itemIds)
                .collectMap(CartItem::getItemId, CartItem::getQuantity);
    }

    public Mono<Integer> getQuantity(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .map(CartItem::getQuantity)
                .defaultIfEmpty(0);
    }

    @Transactional
    public Mono<Void> changeQuantity(long itemId, CartAction action) {
        return itemCache.getCard(itemId).hasElement()
                .flatMap(exists -> exists
                        ? applyAction(itemId, action)
                        : Mono.error(NotFoundException.item(itemId)));
    }

    @Transactional
    public Mono<Void> clear() {
        return cartItemRepository.deleteAll();
    }

    private Flux<CartLine> toCartLines(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            return Flux.empty();
        }
        List<Long> itemIds = cartItems.stream().map(CartItem::getItemId).toList();
        return itemCache.getCards(itemIds)
                .flatMapMany(cards -> Flux.fromIterable(cartItems)
                        .filter(cartItem -> cards.containsKey(cartItem.getItemId()))
                        .map(cartItem -> new CartLine(cards.get(cartItem.getItemId()), cartItem.getQuantity())));
    }

    private Mono<Void> applyAction(long itemId, CartAction action) {
        return switch (action) {
            case PLUS -> increment(itemId);
            case MINUS -> decrement(itemId);
            case DELETE -> cartItemRepository.findByItemId(itemId).flatMap(cartItemRepository::delete);
        };
    }

    private Mono<Void> increment(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItem -> {
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                    return cartItemRepository.save(cartItem);
                })
                .switchIfEmpty(Mono.defer(() -> cartItemRepository.save(new CartItem(itemId, 1))))
                .then();
    }

    private Mono<Void> decrement(long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItem -> {
                    if (cartItem.getQuantity() > 1) {
                        cartItem.setQuantity(cartItem.getQuantity() - 1);
                        return cartItemRepository.save(cartItem).then();
                    }
                    return cartItemRepository.delete(cartItem);
                });
    }
}
