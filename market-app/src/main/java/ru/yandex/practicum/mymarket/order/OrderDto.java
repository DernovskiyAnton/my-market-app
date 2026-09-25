package ru.yandex.practicum.mymarket.order;

import java.util.List;

public record OrderDto(long id, List<OrderItemDto> items, long totalSum) {
}
