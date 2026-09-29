package ru.yandex.practicum.payment.account;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties("payment.account")
public record AccountProperties(@PositiveOrZero long initialBalance, Map<String, @PositiveOrZero Long> balances) {

    public AccountProperties {
        balances = balances == null ? Map.of() : Map.copyOf(balances);
    }

    public long initialBalanceOf(String username) {
        return balances.getOrDefault(username, initialBalance);
    }
}
