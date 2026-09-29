package ru.yandex.practicum.payment.account;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AccountService {

    private final AccountProperties properties;
    private final Map<String, AtomicLong> balances = new ConcurrentHashMap<>();

    public AccountService(AccountProperties properties) {
        this.properties = properties;
    }

    public Mono<Long> getBalance(String username) {
        return Mono.fromSupplier(() -> account(username).get());
    }

    public Mono<Long> withdraw(String username, long amount) {
        if (amount <= 0) {
            return Mono.error(new IllegalArgumentException("Сумма платежа должна быть положительной"));
        }
        return Mono.fromCallable(() -> {
            AtomicLong balance = account(username);
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

    private AtomicLong account(String username) {
        return balances.computeIfAbsent(username, name -> new AtomicLong(properties.initialBalanceOf(name)));
    }
}
