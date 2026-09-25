package ru.yandex.practicum.mymarket.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CartService cartService;

    public Mono<Page<ItemDto>> findItems(String search, SortType sort, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort.toSort());
        boolean hasSearch = StringUtils.hasText(search);
        String query = hasSearch ? search.strip() : "";
        Flux<Item> items = hasSearch ? itemRepository.search(query, pageable) : itemRepository.findAllBy(pageable);
        Mono<Long> total = hasSearch ? itemRepository.countSearch(query) : itemRepository.count();
        return items.collectList()
                .zipWith(total)
                .flatMap(result -> toDtoPage(result.getT1(), pageable, result.getT2()));
    }

    public Mono<ItemDto> getItem(long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> NotFoundException.item(id)))
                .zipWith(cartService.getQuantity(id), ItemMapper::toDto);
    }

    private Mono<Page<ItemDto>> toDtoPage(List<Item> items, Pageable pageable, long total) {
        List<Long> ids = items.stream().map(Item::getId).toList();
        return cartService.getQuantities(ids)
                .map(quantities -> new PageImpl<>(
                        items.stream()
                                .map(item -> ItemMapper.toDto(item, quantities.getOrDefault(item.getId(), 0)))
                                .toList(),
                        pageable, total));
    }
}
