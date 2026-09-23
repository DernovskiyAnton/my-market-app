package ru.yandex.practicum.mymarket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
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
