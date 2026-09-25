package ru.yandex.practicum.mymarket.payment;

public record PaymentAvailability(boolean available, Long balance, String message) {

    public static PaymentAvailability enoughFunds(long balance) {
        return new PaymentAvailability(true, balance, null);
    }

    public static PaymentAvailability insufficientFunds(long balance, long total) {
        return new PaymentAvailability(false, balance,
                "Недостаточно средств на балансе для оплаты заказа: нужно " + total + " руб., доступно "
                        + balance + " руб.");
    }

    public static PaymentAvailability serviceUnavailable() {
        return new PaymentAvailability(false, null, PaymentUnavailableException.MESSAGE);
    }
}
