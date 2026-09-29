package ru.yandex.practicum.payment.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.account.AccountService;
import ru.yandex.practicum.payment.api.PaymentsApiDelegate;
import ru.yandex.practicum.payment.api.model.Balance;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment.api.model.PaymentResult;

@Service
@RequiredArgsConstructor
public class PaymentsApiDelegateImpl implements PaymentsApiDelegate {

    private final AccountService accountService;

    @Override
    public Mono<ResponseEntity<Balance>> getBalance(ServerWebExchange exchange) {
        return accountService.getBalance()
                .map(amount -> ResponseEntity.ok(new Balance(amount)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResult>> pay(Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest
                .flatMap(request -> accountService.withdraw(request.getAmount())
                        .map(balance -> ResponseEntity.ok(new PaymentResult(request.getAmount(), balance))));
    }
}
