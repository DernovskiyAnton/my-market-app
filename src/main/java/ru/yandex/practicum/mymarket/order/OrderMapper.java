package ru.yandex.practicum.mymarket.order;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderDto toDto(Order order) {
        return new OrderDto(order.getId(), order.getItems().stream().map(OrderMapper::toDto).toList(),
                order.getTotalSum());
    }

    public static OrderItemDto toDto(OrderItem orderItem) {
        return new OrderItemDto(orderItem.getItem().getId(), orderItem.getTitle(), orderItem.getPrice(),
                orderItem.getQuantity());
    }
}
