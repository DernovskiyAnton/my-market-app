package ru.yandex.practicum.mymarket.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cart.CartService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    static final int DEFAULT_PAGE_NUMBER = 1;
    static final int DEFAULT_PAGE_SIZE = 5;
    static final int MAX_PAGE_SIZE = 100;

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"/", "/items"})
    public Mono<Rendering> getItems(@RequestParam(defaultValue = "") String search,
                                    @RequestParam(defaultValue = "NO") SortType sort,
                                    @RequestParam(defaultValue = "" + DEFAULT_PAGE_NUMBER) @Min(1) int pageNumber,
                                    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE)
                                    int pageSize) {
        return itemService.findItems(search, sort, pageNumber, pageSize)
                .map(page -> Rendering.view("items")
                        .modelAttribute("items", ItemGrid.toRows(page.getContent()))
                        .modelAttribute("search", search)
                        .modelAttribute("sort", sort.name())
                        .modelAttribute("paging", Paging.of(page))
                        .build());
    }

    @PostMapping("/items")
    public Mono<String> changeCartItemFromCatalog(@Valid @ModelAttribute CatalogCartForm form) {
        String catalogUrl = UriComponentsBuilder.fromPath("/items")
                .queryParam("search", form.search())
                .queryParam("sort", form.sort())
                .queryParam("pageNumber", form.pageNumber())
                .queryParam("pageSize", form.pageSize())
                .encode()
                .toUriString();
        return cartService.changeQuantity(form.id(), form.action())
                .thenReturn("redirect:" + catalogUrl);
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItem(@PathVariable long id) {
        return itemService.getItem(id).map(ItemController::renderItem);
    }

    @PostMapping("/items/{id}")
    public Mono<Rendering> changeCartItemFromItemPage(@PathVariable long id, @Valid @ModelAttribute ItemCartForm form) {
        return cartService.changeQuantity(id, form.action())
                .then(Mono.defer(() -> itemService.getItem(id)))
                .map(ItemController::renderItem);
    }

    private static Rendering renderItem(ItemDto item) {
        return Rendering.view("item").modelAttribute("item", item).build();
    }
}
