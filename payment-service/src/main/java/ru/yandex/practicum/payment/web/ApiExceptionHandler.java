package ru.yandex.practicum.payment.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import ru.yandex.practicum.payment.account.InsufficientFundsException;
import ru.yandex.practicum.payment.api.model.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    public static final String INVALID_REQUEST_MESSAGE = "Некорректный запрос: сумма платежа должна быть положительной";

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException e) {
        log.info("Payment rejected: {}", e.getMessage());
        return error(HttpStatus.CONFLICT, ErrorResponse.CodeEnum.INSUFFICIENT_FUNDS, e.getMessage());
    }

    @ExceptionHandler({ServerWebInputException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        log.info("Invalid payment request: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, ErrorResponse.CodeEnum.INVALID_REQUEST, INVALID_REQUEST_MESSAGE);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, ErrorResponse.CodeEnum code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
