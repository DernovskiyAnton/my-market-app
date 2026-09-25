package ru.yandex.practicum.mymarket.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.mymarket.cart.CartService;
import ru.yandex.practicum.mymarket.common.NotFoundException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final CartService cartService;

    public Page<ItemDto> findItems(String search, SortType sort, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort.toSort());
        Page<Item> items = StringUtils.hasText(search)
                ? itemRepository.search(search.trim(), pageable)
                : itemRepository.findAll(pageable);
        Map<Long, Integer> quantities = cartService.getQuantities(items.map(Item::getId).getContent());
        return items.map(item -> ItemMapper.toDto(item, quantities.getOrDefault(item.getId(), 0)));
    }

    public ItemDto getItem(long id) {
        Item item = itemRepository.findById(id).orElseThrow(() -> NotFoundException.item(id));
        return ItemMapper.toDto(item, cartService.getQuantity(id));
    }
}
