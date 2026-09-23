package ru.yandex.practicum.mymarket.exception;

public class ItemImportException extends RuntimeException {

    public ItemImportException(String message) {
        super(message);
    }

    public ItemImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
