package ru.yandex.practicum.mymarket.payment;

public class PaymentUnavailableException extends RuntimeException {

    public static final String MESSAGE = "Сервис платежей недоступен, оформление заказа временно невозможно";

    public PaymentUnavailableException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
