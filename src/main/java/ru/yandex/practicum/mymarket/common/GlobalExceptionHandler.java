package ru.yandex.practicum.mymarket.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebInputException;
import ru.yandex.practicum.mymarket.itemimport.ItemImportController;
import ru.yandex.practicum.mymarket.itemimport.ItemImportException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERROR_VIEW = "error";
    public static final String BAD_REQUEST_MESSAGE = "Некорректные параметры запроса";
    public static final String UPLOAD_TOO_LARGE_MESSAGE = "Размер загружаемых файлов превышает допустимый";

    @ExceptionHandler(NotFoundException.class)
    public Rendering handleNotFound(NotFoundException e) {
        log.info("Resource not found: {}", e.getMessage());
        return errorView(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    public Rendering handleEmptyCart(EmptyCartException e) {
        log.info("Order rejected: {}", e.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({ServerWebInputException.class, HandlerMethodValidationException.class})
    public Rendering handleBadRequest(Exception e) {
        log.info("Bad request: {}", e.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, BAD_REQUEST_MESSAGE);
    }

    @ExceptionHandler(ItemImportException.class)
    public Rendering handleItemImport(ItemImportException e) {
        log.warn("Item import failed: {}", e.getMessage(), e);
        return importErrorView(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataBufferLimitException.class)
    public Rendering handleUploadTooLarge(DataBufferLimitException e) {
        log.warn("Upload rejected: {}", e.getMessage());
        return importErrorView(HttpStatus.PAYLOAD_TOO_LARGE, UPLOAD_TOO_LARGE_MESSAGE);
    }

    private static Rendering errorView(HttpStatus status, String message) {
        return Rendering.view(ERROR_VIEW)
                .modelAttribute("status", status.value())
                .modelAttribute("error", status.getReasonPhrase())
                .modelAttribute("message", message)
                .status(status)
                .build();
    }

    private static Rendering importErrorView(HttpStatus status, String message) {
        return Rendering.view(ItemImportController.IMPORT_VIEW)
                .modelAttribute("error", message)
                .status(status)
                .build();
    }
}
