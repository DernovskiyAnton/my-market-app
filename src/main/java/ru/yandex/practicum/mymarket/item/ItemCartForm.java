package ru.yandex.practicum.mymarket.item;

import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.mymarket.cart.CartAction;

public record ItemCartForm(@NotNull CartAction action) {
}
