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
                .onErrorMap(this::toPaymentError);
    }

    public Mono<PaymentAvailability> checkAvailability(long total) {
        return getBalance()
                .map(balance -> balance >= total
                        ? PaymentAvailability.enoughFunds(balance)
                        : PaymentAvailability.insufficientFunds(balance, total))
                .onErrorResume(PaymentUnavailableException.class,
                        e -> Mono.just(PaymentAvailability.serviceUnavailable()))
                .onErrorResume(PaymentClientErrorException.class,
                        e -> Mono.just(PaymentAvailability.requestRejected()));
    }

    public Mono<Long> pay(long amount) {
        return paymentsApi.pay(Mono.just(new PaymentRequest(amount)))
                .timeout(properties.timeout())
                .map(PaymentResult::getBalance)
                .onErrorMap(this::isInsufficientFunds, this::toRejected)
                .onErrorMap(error -> !(error instanceof PaymentRejectedException), this::toPaymentError);
    }

    private boolean isInsufficientFunds(Throwable error) {
        return error instanceof WebClientResponseException response
                && response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT);
    }

    private Throwable toRejected(Throwable error) {
        String details = errorMessage((WebClientResponseException) error);
        return new PaymentRejectedException(details == null ? PAYMENT_REJECTED_MESSAGE : "Оплата не прошла: " + details);
    }

    private Throwable toPaymentError(Throwable error) {
        if (error instanceof PaymentUnavailableException || error instanceof PaymentClientErrorException) {
            return error;
        }
        if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
            String details = errorMessage(response);
            log.error("Payment service rejected request {} {} with status {}: {}",
                    requestMethod(response), requestUri(response), response.getStatusCode().value(),
                    response.getResponseBodyAsString());
            return new PaymentClientErrorException(response.getStatusCode().value(), details, error);
        }
        if (error instanceof WebClientResponseException response) {
            log.warn("Payment service responded with server error {}: {}", response.getStatusCode().value(),
                    response.getResponseBodyAsString());
        } else {
            log.warn("Payment service call failed: {}", error.toString());
        }
        return new PaymentUnavailableException(error);
    }

    private static String errorMessage(WebClientResponseException response) {
        try {
            ErrorResponse body = response.getResponseBodyAs(ErrorResponse.class);
            return body == null ? null : body.getMessage();
        } catch (RuntimeException e) {
            log.debug("Cannot parse payment error response", e);
            return null;
        }
    }

    private static Object requestMethod(WebClientResponseException response) {
        return response.getRequest() == null ? "" : response.getRequest().getMethod();
    }

    private static Object requestUri(WebClientResponseException response) {
        return response.getRequest() == null ? "" : response.getRequest().getURI();
    }
}
