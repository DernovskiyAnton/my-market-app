package ru.yandex.practicum.payment.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(new AccountProperties(1000, Map.of("bob", 50L)));
    }

    @Test
    void getBalance_newAccount_startsWithInitialBalance() {
        StepVerifier.create(accountService.getBalance("alice"))
                .expectNext(1000L)
                .verifyComplete();
    }

    @Test
    void getBalance_configuredAccount_startsWithItsOwnBalance() {
        StepVerifier.create(accountService.getBalance("bob"))
                .expectNext(50L)
                .verifyComplete();
    }

    @Test
    void withdraw_decreasesOnlyOwnersBalance() {
        StepVerifier.create(accountService.withdraw("alice", 300))
                .expectNext(700L)
                .verifyComplete();

        StepVerifier.create(accountService.getBalance("alice"))
                .expectNext(700L)
                .verifyComplete();
        StepVerifier.create(accountService.getBalance("carol"))
                .expectNext(1000L)
                .verifyComplete();
    }

    @Test
    void withdraw_wholeBalance_leavesZero() {
        StepVerifier.create(accountService.withdraw("alice", 1000))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void withdraw_moreThanBalance_failsAndKeepsBalance() {
        StepVerifier.create(accountService.withdraw("bob", 51))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(InsufficientFundsException.class)
                        .hasMessageContaining("51")
                        .hasMessageContaining("50"))
                .verify();
        StepVerifier.create(accountService.getBalance("bob"))
                .expectNext(50L)
                .verifyComplete();
    }

    @Test
    void withdraw_nonPositiveAmount_fails() {
        StepVerifier.create(accountService.withdraw("alice", 0))
                .expectError(IllegalArgumentException.class)
                .verify();
        StepVerifier.create(accountService.withdraw("alice", -5))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void withdraw_concurrently_neverGoesBelowZero() {
        long succeeded = Flux.range(0, 50)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(i -> accountService.withdraw("alice", 30)
                        .onErrorResume(InsufficientFundsException.class, e -> Mono.empty()))
                .sequential()
                .count()
                .block();

        assertThat(succeeded).isEqualTo(33);
        StepVerifier.create(accountService.getBalance("alice"))
                .expectNext(10L)
                .verifyComplete();
    }
}
