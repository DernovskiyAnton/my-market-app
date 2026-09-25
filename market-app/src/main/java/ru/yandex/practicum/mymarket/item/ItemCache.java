package ru.yandex.practicum.mymarket.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ItemCache {

    public static final String LIST_KEY = "items:list";
    public static final String CARD_KEY_PREFIX = "items:card:";

    private final ReactiveRedisTemplate<String, ItemCard> cardTemplate;
    private final ReactiveRedisTemplate<String, List<ItemSummary>> listTemplate;
    private final ItemRepository itemRepository;
    private final Duration ttl;

    public ItemCache(ReactiveRedisTemplate<String, ItemCard> cardTemplate,
                     ReactiveRedisTemplate<String, List<ItemSummary>> listTemplate,
                     ItemRepository itemRepository,
                     ItemCacheProperties properties) {
        this.cardTemplate = cardTemplate;
        this.listTemplate = listTemplate;
        this.itemRepository = itemRepository;
        this.ttl = properties.itemsTtl();
    }

    public static String cardKey(long id) {
        return CARD_KEY_PREFIX + id;
    }

    public Mono<List<ItemSummary>> getSummaries() {
        return listTemplate.opsForValue().get(LIST_KEY)
                .onErrorResume(e -> readFailed(LIST_KEY, e))
                .switchIfEmpty(Mono.defer(this::loadSummaries));
    }

    public Mono<ItemCard> getCard(long id) {
        String key = cardKey(id);
        return cardTemplate.opsForValue().get(key)
                .onErrorResume(e -> readFailed(key, e))
                .switchIfEmpty(Mono.defer(() -> itemRepository.findById(id)
                        .map(ItemMapper::toCard)
                        .flatMap(card -> store(cardTemplate, key, card))));
    }

    public Mono<Map<Long, ItemCard>> getCards(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Mono.just(Map.of());
        }
        List<Long> idList = List.copyOf(ids);
        List<String> keys = idList.stream().map(ItemCache::cardKey).toList();
        return cardTemplate.opsForValue().multiGet(keys)
                .onErrorResume(e -> readFailed(String.join(",", keys), e))
                .defaultIfEmpty(List.of())
                .flatMap(cached -> {
                    Map<Long, ItemCard> cards = new HashMap<>();
                    List<Long> missing = new ArrayList<>();
                    for (int i = 0; i < idList.size(); i++) {
                        ItemCard card = i < cached.size() ? cached.get(i) : null;
                        if (card != null) {
                            cards.put(card.id(), card);
                        } else {
                            missing.add(idList.get(i));
                        }
                    }
                    if (missing.isEmpty()) {
                        return Mono.just(cards);
                    }
                    return itemRepository.findAllById(missing)
                            .map(ItemMapper::toCard)
                            .concatMap(card -> store(cardTemplate, cardKey(card.id()), card))
                            .doOnNext(card -> cards.put(card.id(), card))
                            .then(Mono.fromSupplier(() -> Map.copyOf(cards)));
                });
    }

    public Mono<Void> evictSummaries() {
        return listTemplate.delete(LIST_KEY)
                .onErrorResume(e -> {
                    log.warn("Cannot evict {} from Redis: {}", LIST_KEY, e.toString());
                    return Mono.just(0L);
                })
                .then();
    }

    private Mono<List<ItemSummary>> loadSummaries() {
        return itemRepository.findAll(Sort.by("id"))
                .map(ItemMapper::toSummary)
                .collectList()
                .flatMap(summaries -> store(listTemplate, LIST_KEY, summaries));
    }

    private <T> Mono<T> store(ReactiveRedisTemplate<String, T> template, String key, T value) {
        return template.opsForValue().set(key, value, ttl)
                .onErrorResume(e -> {
                    log.warn("Cannot write {} to Redis: {}", key, e.toString());
                    return Mono.just(false);
                })
                .thenReturn(value);
    }

    private <T> Mono<T> readFailed(String key, Throwable error) {
        log.warn("Cannot read {} from Redis, falling back to database: {}", key, error.toString());
        return Mono.empty();
    }
}
