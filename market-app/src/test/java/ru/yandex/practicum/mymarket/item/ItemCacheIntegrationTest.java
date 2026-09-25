package ru.yandex.practicum.mymarket.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.support.IntegrationTestBase;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemCacheIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReactiveRedisTemplate<String, ItemCard> itemCardRedisTemplate;

    @Autowired
    private ReactiveRedisTemplate<String, List<ItemSummary>> itemListRedisTemplate;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemCacheProperties cacheProperties;

    @Test
    void getSummaries_onMiss_loadsFromDatabaseAndStoresListWithTtl() {
        long id = createItem("Кешируемый товар", 700).getId();
        assertThat(itemListRedisTemplate.hasKey(ItemCache.LIST_KEY).block()).isFalse();

        StepVerifier.create(itemCache.getSummaries())
                .assertNext(summaries -> assertThat(summaries)
                        .contains(new ItemSummary(id, "Кешируемый товар", "Описание: Кешируемый товар", 700)))
                .verifyComplete();

        StepVerifier.create(itemListRedisTemplate.opsForValue().get(ItemCache.LIST_KEY))
                .assertNext(cached -> assertThat(cached).extracting(ItemSummary::id).contains(id))
                .verifyComplete();
        assertTtlIsSet(ItemCache.LIST_KEY);
    }

    @Test
    void getCard_onMiss_loadsFromDatabaseAndStoresCardWithTtl() {
        long id = createItem("Кешируемая карточка", 900).getId();

        StepVerifier.create(itemCache.getCard(id))
                .expectNext(new ItemCard(id, "Кешируемая карточка", "Описание: Кешируемая карточка", null, 900))
                .verifyComplete();

        StepVerifier.create(itemCardRedisTemplate.opsForValue().get(ItemCache.cardKey(id)).map(ItemCard::price))
                .expectNext(900L)
                .verifyComplete();
        assertTtlIsSet(ItemCache.cardKey(id));
    }

    @Test
    void itemPage_isServedFromCacheUntilEntryExpiresOrIsEvicted() {
        long id = createItem("Товар до изменения", 100).getId();
        assertThat(getHtml("/items/" + id)).contains("Товар до изменения", "100 руб.");

        updateItemInDatabase(id, "Товар после изменения", 555);

        assertThat(getHtml("/items/" + id)).contains("Товар до изменения", "100 руб.")
                .doesNotContain("Товар после изменения");

        itemCardRedisTemplate.delete(ItemCache.cardKey(id)).block();

        assertThat(getHtml("/items/" + id)).contains("Товар после изменения", "555 руб.");
    }

    @Test
    void catalogPage_isServedFromCachedListUntilItIsEvicted() {
        createItem("Первый кешируемый", 100);
        assertThat(getHtml("/items?search=кешируемый")).contains("Первый кешируемый");

        long secondId = createItemWithoutEviction("Второй кешируемый", 200);

        assertThat(getHtml("/items?search=кешируемый")).doesNotContain("Второй кешируемый");

        itemCache.evictSummaries().block();

        assertThat(getHtml("/items?search=кешируемый")).contains("Первый кешируемый", "Второй кешируемый");
        assertThat(itemCardRedisTemplate.hasKey(ItemCache.cardKey(secondId)).block()).isTrue();
    }

    @Test
    void getCards_mixesCachedAndDatabaseEntries() {
        long cachedId = createItem("Уже в кеше", 10).getId();
        long missingId = createItem("Ещё не в кеше", 20).getId();
        itemCardRedisTemplate.opsForValue()
                .set(ItemCache.cardKey(cachedId), new ItemCard(cachedId, "Из Redis", "", null, 11), Duration.ofMinutes(1))
                .block();

        StepVerifier.create(itemCache.getCards(List.of(cachedId, missingId)))
                .assertNext(cards -> {
                    assertThat(cards.get(cachedId).title()).isEqualTo("Из Redis");
                    assertThat(cards.get(missingId).title()).isEqualTo("Ещё не в кеше");
                })
                .verifyComplete();
        assertThat(itemCardRedisTemplate.hasKey(ItemCache.cardKey(missingId)).block()).isTrue();
    }

    @Test
    void cartPage_readsItemsFromCache() {
        long id = createItem("Товар в корзине", 300).getId();
        webTestClient.post().uri("/cart/items?id=" + id + "&action=PLUS").exchange().expectStatus().isOk();

        assertThat(itemCardRedisTemplate.hasKey(ItemCache.cardKey(id)).block()).isTrue();
        updateItemInDatabase(id, "Переименованный товар", 300);

        assertThat(getHtml("/cart/items")).contains("Товар в корзине").doesNotContain("Переименованный товар");
    }

    @Test
    void getCard_unknownItem_isNotCached() {
        StepVerifier.create(itemCache.getCard(-42L)).verifyComplete();

        assertThat(itemCardRedisTemplate.hasKey(ItemCache.cardKey(-42L)).block()).isFalse();
        StepVerifier.create(itemService.getItem(-42L)).expectError().verify();
    }

    private long createItemWithoutEviction(String title, long price) {
        long id = itemRepository.save(new Item(title, "Описание: " + title, null, price)).map(Item::getId).block();
        trackCreatedItems(title);
        return id;
    }

    private void updateItemInDatabase(long id, String title, long price) {
        databaseClient.sql("UPDATE items SET title = :title, price = :price WHERE id = :id")
                .bind("title", title)
                .bind("price", price)
                .bind("id", id)
                .then()
                .block();
    }

    private void assertTtlIsSet(String key) {
        Duration ttl = itemCardRedisTemplate.getExpire(key).block();
        assertThat(ttl).isPositive().isLessThanOrEqualTo(cacheProperties.itemsTtl());
    }

    private String getHtml(String uri) {
        return webTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }
}
