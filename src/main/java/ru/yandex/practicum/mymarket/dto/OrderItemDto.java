package ru.yandex.practicum.mymarket.dto;

/**
 * Позиция заказа.
 *
 * @param count количество товара в заказе
 */
public record OrderItemDto(long id, String title, long price, int count) {
}
