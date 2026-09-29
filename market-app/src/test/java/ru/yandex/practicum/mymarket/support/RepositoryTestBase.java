package ru.yandex.practicum.mymarket.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.user.Role;
import ru.yandex.practicum.mymarket.user.User;
import ru.yandex.practicum.mymarket.user.UserRepository;

@DataR2dbcTest(properties = "spring.r2dbc.url=r2dbc:h2:mem:///repository-tests;DB_CLOSE_DELAY=-1")
public abstract class RepositoryTestBase {

    @Autowired
    protected DatabaseClient databaseClient;

    @Autowired
    protected UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        Flux.just("order_items", "orders", "cart_items", "items", "users")
                .concatMap(table -> databaseClient.sql("DELETE FROM " + table).then())
                .blockLast();
    }

    protected Long createUser(String username) {
        return userRepository.save(new User(username, "{bcrypt}hash", Role.USER)).map(User::getId).block();
    }
}
