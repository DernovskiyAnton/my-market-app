package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemCacheTest {

    private static final Duration TTL = Duration.ofMinutes(2);

    @Mock
    private ReactiveRedisTemplate<String, ItemCard> cardTemplate;

    @Mock
    private ReactiveRedisTemplate<String, List<ItemSummary>> listTemplate;

    @Mock
    private ReactiveValueOperations<String, ItemCard> cardOps;

    @Mock
    private ReactiveValueOperations<String, List<ItemSummary>> listOps;

    @Mock
    private ItemRepository itemRepository;

    private ItemCache itemCache;

    @BeforeEach
    void setUp() {
        itemCache = new ItemCache(cardTemplate, listTemplate, itemRepository, new ItemCacheProperties(TTL));
    }

    @Test
    void getCard_hit_doesNotQueryDatabase() {
        ItemCard card = new ItemCard(1L, "Мяч", "d", "images/ball.svg", 100);
        when(cardTemplate.opsForValue()).thenReturn(cardOps);
        when(cardOps.get("items:card:1")).thenReturn(Mono.just(card));

        StepVerifier.create(itemCache.getCard(1L))
                .expectNext(card)
                .verifyComplete();
        verifyNoInteractions(itemRepository);
    }

    @Test
    void getCard_miss_loadsFromDatabaseAndStoresWithTtl() {
        when(cardTemplate.opsForValue()).thenReturn(cardOps);
        when(cardOps.get("items:card:1")).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 100)));
        when(cardOps.set(eq("items:card:1"), any(ItemCard.class), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(itemCache.getCard(1L))
                .expectNext(new ItemCard(1L, "Мяч", "Описание", "images/1.svg", 100))
                .verifyComplete();
        verify(cardOps).set("items:card:1", new ItemCard(1L, "Мяч", "Описание", "images/1.svg", 100), TTL);
    }

    @Test
    void getCard_redisUnavailable_fallsBackToDatabase() {
        when(cardTemplate.opsForValue()).thenReturn(cardOps);
        when(cardOps.get("items:card:1")).thenReturn(Mono.error(new RedisConnectionFailureException("down")));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1L, "Мяч", 100)));
        when(cardOps.set(anyString(), any(ItemCard.class), eq(TTL)))
                .thenReturn(Mono.error(new RedisConnectionFailureException("down")));

        StepVerifier.create(itemCache.getCard(1L).map(ItemCard::title))
                .expectNext("Мяч")
                .verifyComplete();
    }

    @Test
    void getSummaries_redisUnavailable_fallsBackToDatabase() {
        when(listTemplate.opsForValue()).thenReturn(listOps);
        when(listOps.get("items:list")).thenReturn(Mono.error(new RedisConnectionFailureException("down")));
        when(itemRepository.findAll(Sort.by("id"))).thenReturn(Flux.just(item(1L, "Мяч", 100), item(2L, "Скакалка", 50)));
        when(listOps.set(eq("items:list"), anyList(), eq(TTL)))
                .thenReturn(Mono.error(new RedisConnectionFailureException("down")));

        StepVerifier.create(itemCache.getSummaries())
                .expectNext(List.of(new ItemSummary(1L, "Мяч", "Описание", 100),
                        new ItemSummary(2L, "Скакалка", "Описание", 50)))
                .verifyComplete();
    }

    @Test
    void getCards_loadsOnlyMissingCardsFromDatabase() {
        ItemCard cached = new ItemCard(1L, "Из кеша", "d", null, 10);
        when(cardTemplate.opsForValue()).thenReturn(cardOps);
        when(cardOps.multiGet(List.of("items:card:1", "items:card:2"))).thenReturn(Mono.just(Arrays.asList(cached, null)));
        when(itemRepository.findAllById(List.of(2L))).thenReturn(Flux.just(item(2L, "Из базы", 20)));
        when(cardOps.set(eq("items:card:2"), any(ItemCard.class), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(itemCache.getCards(List.of(1L, 2L)))
                .expectNext(Map.of(1L, cached, 2L, new ItemCard(2L, "Из базы", "Описание", "images/2.svg", 20)))
                .verifyComplete();
    }

    @Test
    void getCards_emptyIds_doesNotTouchRedisOrDatabase() {
        StepVerifier.create(itemCache.getCards(List.of()))
                .expectNext(Map.of())
                .verifyComplete();
        verifyNoInteractions(cardTemplate, itemRepository);
    }

    @Test
    void evictSummaries_redisUnavailable_completesWithoutError() {
        when(listTemplate.delete("items:list")).thenReturn(Mono.error(new RedisConnectionFailureException("down")));

        StepVerifier.create(itemCache.evictSummaries()).verifyComplete();
        verify(listTemplate, never()).opsForValue();
    }

    private static Item item(long id, String title, long price) {
        Item item = new Item(title, "Описание", "images/" + id + ".svg", price);
        item.setId(id);
        return item;
    }
}
