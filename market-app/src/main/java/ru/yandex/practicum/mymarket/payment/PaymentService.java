package ru.yandex.practicum.mymarket.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.payment.client.api.PaymentsApi;
import ru.yandex.practicum.mymarket.payment.client.model.Balance;
import ru.yandex.practicum.mymarket.payment.client.model.ErrorResponse;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentRequest;
import ru.yandex.practicum.mymarket.payment.client.model.PaymentResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    static final String PAYMENT_REJECTED_MESSAGE = "Оплата не прошла: недостаточно средств на балансе";

    private final PaymentsApi paymentsApi;
    private final PaymentProperties properties;

    public Mono<Long> getBalance() {
        return paymentsApi.getBalance()
                .timeout(properties.timeout())
                .map(Balance::getAmount)
                .onErrorMap(this::toUnavailable);
    }

    public Mono<PaymentAvailability> checkAvailability(long total) {
        return getBalance()
                .map(balance -> balance >= total
                        ? PaymentAvailability.enoughFunds(balance)
                        : PaymentAvailability.insufficientFunds(balance, total))
                .onErrorResume(PaymentUnavailableException.class,
                        e -> Mono.just(PaymentAvailability.serviceUnavailable()));
    }

    public Mono<Long> pay(long amount) {
        return paymentsApi.pay(Mono.just(new PaymentRequest(amount)))
                .timeout(properties.timeout())
                .map(PaymentResult::getBalance)
                .onErrorMap(this::isRejected, this::toRejected)
                .onErrorMap(error -> !(error instanceof PaymentRejectedException), this::toUnavailable);
    }

    private boolean isRejected(Throwable error) {
        return error instanceof WebClientResponseException response
                && response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT);
    }

    private Throwable toRejected(Throwable error) {
        WebClientResponseException response = (WebClientResponseException) error;
        String message = PAYMENT_REJECTED_MESSAGE;
        try {
            ErrorResponse body = response.getResponseBodyAs(ErrorResponse.class);
            if (body != null && body.getMessage() != null) {
                message = "Оплата не прошла: " + body.getMessage();
            }
        } catch (RuntimeException e) {
            log.debug("Cannot parse payment error response", e);
        }
        return new PaymentRejectedException(message);
    }

    private Throwable toUnavailable(Throwable error) {
        if (error instanceof PaymentUnavailableException) {
            return error;
        }
        log.warn("Payment service call failed: {}", error.toString());
        return new PaymentUnavailableException(error);
    }
}
