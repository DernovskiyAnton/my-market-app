package ru.yandex.practicum.mymarket.cart;

import ru.yandex.practicum.mymarket.item.Item;

public record CartLine(Item item, int quantity) {
}
