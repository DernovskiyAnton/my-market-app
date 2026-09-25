package ru.yandex.practicum.mymarket.order;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderDto toDto(Order order, List<OrderItem> items) {
        return new OrderDto(order.getId(), items.stream().map(OrderMapper::toDto).toList(), order.getTotalSum());
    }

    public static OrderItemDto toDto(OrderItem orderItem) {
        return new OrderItemDto(orderItem.getItemId(), orderItem.getTitle(), orderItem.getPrice(),
                orderItem.getQuantity());
    }
}
