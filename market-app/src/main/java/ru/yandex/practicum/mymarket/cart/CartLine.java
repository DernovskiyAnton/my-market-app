package ru.yandex.practicum.mymarket.cart;

import ru.yandex.practicum.mymarket.item.ItemCard;

public record CartLine(ItemCard item, int quantity) {
}
