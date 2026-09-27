package ru.yandex.practicum.mymarket.payment;

public class PaymentClientErrorException extends RuntimeException {

    public static final String MESSAGE = "Сервис платежей отклонил запрос, оплата временно невозможна";

    private final int status;

    public PaymentClientErrorException(int status, String details, Throwable cause) {
        super(MESSAGE + (details == null ? "" : ": " + details), cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
