package ru.yandex.practicum.mymarket.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.exception.ItemImportException;

import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERROR_VIEW = "error";
    public static final String IMPORT_PAGE_REDIRECT = "redirect:/admin/items";
    public static final String BAD_REQUEST_MESSAGE = "Некорректные параметры запроса";
    public static final String UPLOAD_TOO_LARGE_MESSAGE = "Размер загружаемых файлов превышает допустимый";

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException e) {
        log.info("Resource not found: {}", e.getMessage());
        return errorView(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    public ModelAndView handleEmptyCart(EmptyCartException e) {
        log.info("Order rejected: {}", e.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HandlerMethodValidationException.class
    })
    public ModelAndView handleBadRequest(Exception e) {
        log.info("Bad request: {}", e.getMessage());
        return errorView(HttpStatus.BAD_REQUEST, BAD_REQUEST_MESSAGE);
    }

    @ExceptionHandler(ItemImportException.class)
    public String handleItemImport(ItemImportException e, RedirectAttributes redirectAttributes) {
        log.warn("Item import failed: {}", e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return IMPORT_PAGE_REDIRECT;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e, RedirectAttributes redirectAttributes) {
        log.warn("Upload rejected: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error", UPLOAD_TOO_LARGE_MESSAGE);
        return IMPORT_PAGE_REDIRECT;
    }

    private static ModelAndView errorView(HttpStatus status, String message) {
        return new ModelAndView(ERROR_VIEW,
                Map.of("status", status.value(), "error", status.getReasonPhrase(), "message", message),
                status);
    }
}
