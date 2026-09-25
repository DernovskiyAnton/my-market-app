package ru.yandex.practicum.mymarket.itemimport;

public class ItemImportException extends RuntimeException {

    public ItemImportException(String message) {
        super(message);
    }

    public ItemImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
