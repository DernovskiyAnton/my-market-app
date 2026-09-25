package ru.yandex.practicum.mymarket.common;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Невозможно оформить заказ: корзина пуста");
    }
}
