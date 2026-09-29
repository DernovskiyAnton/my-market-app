package ru.yandex.practicum.mymarket.item;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("market.cache")
public record ItemCacheProperties(@NotNull Duration itemsTtl) {
}
