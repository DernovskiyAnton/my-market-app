package ru.yandex.practicum.mymarket.item;

import ru.yandex.practicum.mymarket.model.CartItem;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemDto toDto(Item item, int count) {
        return new ItemDto(item.getId(), item.getTitle(), item.getDescription(), item.getImgPath(),
                item.getPrice(), count);
    }

    public static ItemDto toDto(CartItem cartItem) {
        return toDto(cartItem.getItem(), cartItem.getQuantity());
    }
}
