package ru.yandex.practicum.payment.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(new AccountProperties(1000));
    }

    @Test
    void getBalance_returnsInitialBalance() {
        StepVerifier.create(accountService.getBalance())
                .expectNext(1000L)
                .verifyComplete();
    }

    @Test
    void withdraw_decreasesBalance() {
        StepVerifier.create(accountService.withdraw(300))
                .expectNext(700L)
                .verifyComplete();
        StepVerifier.create(accountService.getBalance())
                .expectNext(700L)
                .verifyComplete();
    }

    @Test
    void withdraw_wholeBalance_leavesZero() {
        StepVerifier.create(accountService.withdraw(1000))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void withdraw_moreThanBalance_failsAndKeepsBalance() {
        StepVerifier.create(accountService.withdraw(1001))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(InsufficientFundsException.class)
                        .hasMessageContaining("1001")
                        .hasMessageContaining("1000"))
                .verify();
        StepVerifier.create(accountService.getBalance())
                .expectNext(1000L)
                .verifyComplete();
    }

    @Test
    void withdraw_nonPositiveAmount_fails() {
        StepVerifier.create(accountService.withdraw(0))
                .expectError(IllegalArgumentException.class)
                .verify();
        StepVerifier.create(accountService.withdraw(-5))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void withdraw_concurrently_neverGoesBelowZero() {
        long succeeded = Flux.range(0, 50)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(i -> accountService.withdraw(30).onErrorResume(InsufficientFundsException.class,
                        e -> Mono.empty()))
                .sequential()
                .count()
                .block();

        assertThat(succeeded).isEqualTo(33);
        StepVerifier.create(accountService.getBalance())
                .expectNext(10L)
                .verifyComplete();
    }
}
