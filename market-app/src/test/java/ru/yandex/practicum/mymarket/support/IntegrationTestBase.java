package ru.yandex.practicum.mymarket.support;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.item.Item;
import ru.yandex.practicum.mymarket.item.ItemCache;
import ru.yandex.practicum.mymarket.item.ItemRepository;
import ru.yandex.practicum.mymarket.user.MarketUser;
import ru.yandex.practicum.mymarket.user.UserRepository;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@SpringBootTest(properties = "market.images.dir=target/test-images")
@AutoConfigureWebTestClient
public abstract class IntegrationTestBase {

    protected static final long DEFAULT_BALANCE = 1_000_000;

    @ServiceConnection
    protected static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));

    protected static final PaymentServerStub PAYMENT_SERVER = new PaymentServerStub();

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void paymentServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("market.payment.base-url", PAYMENT_SERVER::baseUrl);
        registry.add("market.payment.timeout", () -> "2s");
        registry.add("spring.security.oauth2.client.provider.market-auth.token-uri", PAYMENT_SERVER::tokenUri);
        registry.add("spring.security.oauth2.client.registration.payment-service.client-id",
                () -> PaymentServerStub.CLIENT_ID);
        registry.add("spring.security.oauth2.client.registration.payment-service.client-secret",
                () -> PaymentServerStub.CLIENT_SECRET);
    }

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected DatabaseClient databaseClient;

    @Autowired
    protected ReactiveRedisConnectionFactory redisConnectionFactory;

    @Autowired
    protected ItemCache itemCache;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    private final List<Long> createdItemIds = new ArrayList<>();

    @BeforeEach
    void resetExternalServices() {
        PAYMENT_SERVER.reset(DEFAULT_BALANCE);
        forgetPaymentServiceToken();
        flushRedis();
    }

    @AfterEach
    void cleanDatabase() {
        Flux.just("order_items", "orders", "cart_items")
                .concatMap(table -> databaseClient.sql("DELETE FROM " + table).then())
                .then(itemRepository.deleteAllById(createdItemIds))
                .block();
        createdItemIds.clear();
        flushRedis();
    }

    protected Item createItem(String title, long price) {
        Item item = itemRepository.save(new Item(title, "Описание: " + title, null, price)).block();
        createdItemIds.add(item.getId());
        itemCache.evictSummaries().block();
        return item;
    }

    protected void trackCreatedItems(String titlePart) {
        itemRepository.findAll()
                .filter(item -> item.getTitle().contains(titlePart))
                .map(Item::getId)
                .doOnNext(createdItemIds::add)
                .blockLast();
    }

    protected MarketUser user(String username) {
        return userRepository.findByUsername(username).map(MarketUser::of).blockOptional()
                .orElseThrow(() -> new IllegalStateException("Нет пользователя " + username));
    }

    protected WebTestClient as(String username) {
        MarketUser user = user(username);
        return webTestClient
                .mutateWith(mockAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())))
                .mutateWith(csrf());
    }

    protected WebTestClient asAlice() {
        return as("alice");
    }

    protected void forgetPaymentServiceToken() {
        authorizedClientService.removeAuthorizedClient("payment-service", "payment-service").block();
    }

    protected void flushRedis() {
        redisConnectionFactory.getReactiveConnection().serverCommands().flushAll().block();
    }
}
