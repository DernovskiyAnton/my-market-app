package ru.yandex.practicum.mymarket.payment;

public class PaymentRejectedException extends RuntimeException {

    public PaymentRejectedException(String message) {
        super(message);
    }
}
