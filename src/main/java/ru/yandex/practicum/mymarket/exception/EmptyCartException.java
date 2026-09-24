package ru.yandex.practicum.mymarket.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Невозможно оформить заказ: корзина пуста");
    }
}
