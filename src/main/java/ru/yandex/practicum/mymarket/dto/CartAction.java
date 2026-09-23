package ru.yandex.practicum.mymarket.dto;

/**
 * Действие с товаром в корзине.
 */
public enum CartAction {
    /** Увеличить количество на один. */
    PLUS,
    /** Уменьшить количество на один (при нуле товар удаляется из корзины). */
    MINUS,
    /** Удалить товар из корзины. */
    DELETE
}
