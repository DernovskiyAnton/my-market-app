package ru.yandex.practicum.mymarket.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemCache itemCache;
    private final CartService cartService;

    public Mono<Page<ItemDto>> findItems(String search, SortType sort, int pageNumber, int pageSize) {
        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize);
        return itemCache.getSummaries().flatMap(summaries -> {
            List<ItemSummary> found = summaries.stream()
                    .filter(matches(search))
                    .sorted(sort.comparator())
                    .toList();
            List<Long> pageIds = found.stream()
                    .skip(pageable.getOffset())
                    .limit(pageSize)
                    .map(ItemSummary::id)
                    .toList();
            return Mono.zip(itemCache.getCards(pageIds), cartService.getQuantities(pageIds))
                    .map(result -> new PageImpl<>(toDtos(pageIds, result.getT1(), result.getT2()),
                            pageable, found.size()));
        });
    }

    public Mono<ItemDto> getItem(long id) {
        return itemCache.getCard(id)
                .switchIfEmpty(Mono.error(() -> NotFoundException.item(id)))
                .zipWith(cartService.getQuantity(id), ItemMapper::toDto);
    }

    private static Predicate<ItemSummary> matches(String search) {
        if (!StringUtils.hasText(search)) {
            return summary -> true;
        }
        String query = search.strip().toLowerCase(Locale.ROOT);
        return summary -> summary.title().toLowerCase(Locale.ROOT).contains(query)
                || summary.description().toLowerCase(Locale.ROOT).contains(query);
    }

    private static List<ItemDto> toDtos(List<Long> ids, Map<Long, ItemCard> cards, Map<Long, Integer> quantities) {
        return ids.stream()
                .filter(cards::containsKey)
                .map(id -> ItemMapper.toDto(cards.get(id), quantities.getOrDefault(id, 0)))
                .toList();
    }
}
