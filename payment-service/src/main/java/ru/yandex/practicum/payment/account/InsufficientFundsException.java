package ru.yandex.practicum.payment.account;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(long amount, long balance) {
        super("Недостаточно средств на балансе: требуется " + amount + " руб., доступно " + balance + " руб.");
    }
}
