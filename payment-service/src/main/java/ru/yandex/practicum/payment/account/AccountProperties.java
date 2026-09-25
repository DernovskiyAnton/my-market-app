package ru.yandex.practicum.payment.account;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("payment.account")
public record AccountProperties(@PositiveOrZero long initialBalance) {
}
