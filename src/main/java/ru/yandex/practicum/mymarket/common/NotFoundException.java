package ru.yandex.practicum.mymarket.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException item(long id) {
        return new NotFoundException("Товар с id=" + id + " не найден");
    }

    public static NotFoundException order(long id) {
        return new NotFoundException("Заказ с id=" + id + " не найден");
    }
}
