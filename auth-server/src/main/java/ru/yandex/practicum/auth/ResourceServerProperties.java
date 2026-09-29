package ru.yandex.practicum.auth;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Set;

@Validated
@ConfigurationProperties("auth")
public record ResourceServerProperties(@NotEmpty Map<String, Set<String>> resourceServers) {
}
