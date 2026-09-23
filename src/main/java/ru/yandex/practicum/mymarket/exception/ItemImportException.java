package ru.yandex.practicum.mymarket.exception;

/**
 * Ошибка разбора или сохранения данных при импорте товаров.
 */
public class ItemImportException extends RuntimeException {

    public ItemImportException(String message) {
        super(message);
    }

    public ItemImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
