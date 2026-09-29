package ru.yandex.practicum.mymarket.cart;

import jakarta.validation.constraints.NotNull;

public record CartItemForm(@NotNull Long id, @NotNull CartAction action) {
}
