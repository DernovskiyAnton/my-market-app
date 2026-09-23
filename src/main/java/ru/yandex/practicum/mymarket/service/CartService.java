package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Управление содержимым корзины покупателя.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartDto getCart() {
        List<ItemDto> items = cartItemRepository.findAllByOrderByIdAsc().stream()
                .map(ItemMapper::toDto)
                .toList();
        long total = items.stream().mapToLong(item -> item.price() * item.count()).sum();
        return new CartDto(items, total);
    }

    /**
     * Возвращает количество в корзине для каждого из переданных товаров.
     * Товары, которых нет в корзине, в результат не попадают.
     */
    public Map<Long, Integer> getQuantities(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return cartItemRepository.findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(cartItem -> cartItem.getItem().getId(), CartItem::getQuantity));
    }

    public int getQuantity(long itemId) {
        return cartItemRepository.findByItemId(itemId).map(CartItem::getQuantity).orElse(0);
    }

    @Transactional
    public void changeQuantity(long itemId, CartAction action) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> NotFoundException.item(itemId));
        switch (action) {
            case PLUS -> increment(item);
            case MINUS -> decrement(itemId);
            case DELETE -> cartItemRepository.findByItemId(itemId).ifPresent(cartItemRepository::delete);
        }
    }

    @Transactional
    public void clear() {
        cartItemRepository.deleteAllInBatch();
    }

    private void increment(Item item) {
        cartItemRepository.findByItemId(item.getId()).ifPresentOrElse(
                cartItem -> cartItem.setQuantity(cartItem.getQuantity() + 1),
                () -> cartItemRepository.save(new CartItem(item, 1)));
    }

    private void decrement(long itemId) {
        cartItemRepository.findByItemId(itemId).ifPresent(cartItem -> {
            if (cartItem.getQuantity() > 1) {
                cartItem.setQuantity(cartItem.getQuantity() - 1);
            } else {
                cartItemRepository.delete(cartItem);
            }
        });
    }
}
