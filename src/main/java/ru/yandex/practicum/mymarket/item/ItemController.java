package ru.yandex.practicum.mymarket.item;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.dto.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private static final String DEFAULT_PAGE_NUMBER = "1";
    private static final String DEFAULT_PAGE_SIZE = "5";
    private static final int MAX_PAGE_SIZE = 100;

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"/", "/items"})
    public String getItems(@RequestParam(defaultValue = "") String search,
                           @RequestParam(defaultValue = "NO") SortType sort,
                           @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) @Min(1) int pageNumber,
                           @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int pageSize,
                           Model model) {
        Page<ItemDto> page = itemService.findItems(search, sort, pageNumber, pageSize);
        model.addAttribute("items", ItemGrid.toRows(page.getContent()));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort.name());
        model.addAttribute("paging", Paging.of(page));
        return "items";
    }

    @PostMapping("/items")
    public String changeCartItemFromCatalog(@RequestParam long id,
                                            @RequestParam(defaultValue = "") String search,
                                            @RequestParam(defaultValue = "NO") SortType sort,
                                            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) @Min(1) int pageNumber,
                                            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int pageSize,
                                            @RequestParam CartAction action,
                                            RedirectAttributes redirectAttributes) {
        cartService.changeQuantity(id, action);
        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        redirectAttributes.addAttribute("pageSize", pageSize);
        return "redirect:/items";
    }

    @GetMapping("/items/{id}")
    public String getItem(@PathVariable long id, Model model) {
        model.addAttribute("item", itemService.getItem(id));
        return "item";
    }

    @PostMapping("/items/{id}")
    public String changeCartItemFromItemPage(@PathVariable long id,
                                             @RequestParam CartAction action,
                                             Model model) {
        cartService.changeQuantity(id, action);
        model.addAttribute("item", itemService.getItem(id));
        return "item";
    }
}
