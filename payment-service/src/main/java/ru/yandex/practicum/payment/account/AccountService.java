package ru.yandex.practicum.payment.account;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class AccountService {

    private final AtomicLong balance;

    public AccountService(AccountProperties properties) {
        this.balance = new AtomicLong(properties.initialBalance());
    }

    public Mono<Long> getBalance() {
        return Mono.fromSupplier(balance::get);
    }

    public Mono<Long> withdraw(long amount) {
        if (amount <= 0) {
            return Mono.error(new IllegalArgumentException("Сумма платежа должна быть положительной"));
        }
        return Mono.fromCallable(() -> {
            long current;
            do {
                current = balance.get();
                if (current - amount < 0) {
                    throw new InsufficientFundsException(amount, current);
                }
            } while (!balance.compareAndSet(current, current - amount));
            return current - amount;
        });
    }
}
