package ru.yandex.practicum.mymarket.dto;

import java.util.List;

/**
 * Содержимое корзины и суммарная стоимость её товаров.
 */
public record CartDto(List<ItemDto> items, long total) {
}
