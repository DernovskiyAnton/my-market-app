package ru.yandex.practicum.mymarket.user;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.support.RepositoryTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends RepositoryTestBase {

    @Test
    void findByUsername_returnsSavedUser() {
        userRepository.save(new User("carol", "$2a$10$hash", Role.ADMIN)).block();

        StepVerifier.create(userRepository.findByUsername("carol"))
                .assertNext(user -> {
                    assertThat(user.getId()).isNotNull();
                    assertThat(user.getPassword()).isEqualTo("$2a$10$hash");
                    assertThat(user.getRole()).isEqualTo(Role.ADMIN);
                })
                .verifyComplete();
        StepVerifier.create(userRepository.findByUsername("nobody")).verifyComplete();
    }

    @Test
    void save_duplicateUsername_violatesUniqueConstraint() {
        createUser("carol");

        StepVerifier.create(userRepository.save(new User("carol", "other", Role.USER)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}
